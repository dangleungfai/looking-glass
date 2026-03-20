"""SSH executor: connect to device and run command, return raw output."""

import socket
import time
from typing import Optional

import paramiko
from paramiko.ssh_exception import AuthenticationException, NoValidConnectionsError


def execute(
    host: str,
    port: int,
    username: str,
    command: str,
    password: Optional[str] = None,
    private_key: Optional[str] = None,
    timeout_sec: int = 10,
) -> tuple[str, Optional[str]]:
    """
    Run command on device via SSH. Returns (raw_output, error_message).
    """
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        connect_kw: dict = {
            "hostname": host,
            "port": port,
            "username": username,
            "timeout": min(timeout_sec, 15),
            "allow_agent": False,
            "look_for_keys": False,
        }
        if password:
            connect_kw["password"] = password
        elif private_key:
            from io import StringIO
            pkey = paramiko.RSAKey.from_private_key(StringIO(private_key))
            connect_kw["pkey"] = pkey
        else:
            return "", "No password or private key provided"

        client.connect(**connect_kw)
        chan = client.get_transport().open_session()
        chan.settimeout(timeout_sec)
        chan.get_pty(width=256)
        chan.exec_command(command)

        stdout_chunks: list[str] = []
        stderr_chunks: list[str] = []

        # 循环读取，直到通道关闭或超时
        end_time = time.time() + timeout_sec
        while True:
            if chan.recv_ready():
                data = chan.recv(65535)
                if not data:
                    break
                stdout_chunks.append(data.decode("utf-8", errors="replace"))
            if chan.recv_stderr_ready():
                data = chan.recv_stderr(65535)
                if not data:
                    break
                stderr_chunks.append(data.decode("utf-8", errors="replace"))
            if chan.exit_status_ready():
                # 再尝试把缓冲区读干净
                while chan.recv_ready():
                    data = chan.recv(65535)
                    if not data:
                        break
                    stdout_chunks.append(data.decode("utf-8", errors="replace"))
                while chan.recv_stderr_ready():
                    data = chan.recv_stderr(65535)
                    if not data:
                        break
                    stderr_chunks.append(data.decode("utf-8", errors="replace"))
                break
            if time.time() > end_time:
                raise socket.timeout("SSH read timeout")
            # 避免 busy loop
            time.sleep(0.05)

        chan.close()
        stdout = "".join(stdout_chunks)
        stderr = "".join(stderr_chunks)
        out = (stdout + ("\n" + stderr if stderr else "")).strip()
        return out, None
    except AuthenticationException:
        return "", "AUTHENTICATION_FAILED: username/password rejected by device"
    except NoValidConnectionsError as e:
        # Usually means TCP unreachable/refused for target port.
        return "", f"PORT_UNREACHABLE: cannot connect to {host}:{port} ({e!s})"
    except socket.timeout:
        return "", f"NETWORK_TIMEOUT: timeout connecting to {host}:{port}"
    except socket.gaierror as e:
        return "", f"DNS_RESOLVE_FAILED: {e!s}"
    except OSError as e:
        msg = str(e).lower()
        if "no route to host" in msg or "network is unreachable" in msg:
            return "", f"NETWORK_UNREACHABLE: {e!s}"
        if "connection refused" in msg:
            return "", f"PORT_REFUSED: {e!s}"
        return "", f"NETWORK_ERROR: {e!s}"
    except paramiko.SSHException as e:
        return "", f"SSH error: {e!s}"
    except Exception as e:
        return "", f"Error: {e!s}"
    finally:
        try:
            client.close()
        except Exception:
            pass
