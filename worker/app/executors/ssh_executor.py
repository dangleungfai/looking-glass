"""SSH executor: connect to device and run command, return raw output."""

import socket
import time
from typing import Optional

import paramiko


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

        stdout = chan.recv(65535).decode("utf-8", errors="replace")
        stderr = chan.recv_stderr(65535).decode("utf-8", errors="replace")
        chan.close()
        out = (stdout + "\n" + stderr).strip()
        return out, None
    except socket.timeout:
        return "", "SSH connection timeout"
    except paramiko.SSHException as e:
        return "", f"SSH error: {e!s}"
    except Exception as e:
        return "", f"Error: {e!s}"
    finally:
        try:
            client.close()
        except Exception:
            pass
