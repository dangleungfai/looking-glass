import time
from typing import Any, Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from app.executors.ssh_executor import execute as ssh_execute
from app.parsers.parsers import parse_output

app = FastAPI(title="ISP Looking Glass Worker")


class ExecuteRequest(BaseModel):
    requestId: str
    mgmtIp: str
    sshPort: int = 22
    username: str
    password: Optional[str] = None
    privateKey: Optional[str] = None
    command: str
    queryType: str
    timeoutSec: int = 10
    resultShape: Optional[dict[str, Any]] = None


class ExecuteResponse(BaseModel):
    requestId: str
    status: str
    durationMs: Optional[int] = None
    rawText: Optional[str] = None
    result: Optional[dict[str, Any]] = None
    errorMessage: Optional[str] = None


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.post("/execute", response_model=ExecuteResponse)
async def execute(req: ExecuteRequest):
    start = time.perf_counter()
    try:
        raw, err = ssh_execute(
            host=req.mgmtIp,
            port=req.sshPort,
            username=req.username,
            password=req.password,
            private_key=req.privateKey,
            command=req.command,
            timeout_sec=req.timeoutSec,
        )
        duration_ms = int((time.perf_counter() - start) * 1000)
        if err:
            return ExecuteResponse(
                requestId=req.requestId,
                status="FAILED",
                durationMs=duration_ms,
                rawText=raw or None,
                errorMessage=err,
            )
        result = parse_output(req.queryType, raw)
        return ExecuteResponse(
            requestId=req.requestId,
            status="SUCCESS",
            durationMs=duration_ms,
            rawText=raw,
            result=result,
        )
    except Exception as e:
        duration_ms = int((time.perf_counter() - start) * 1000)
        return ExecuteResponse(
            requestId=req.requestId,
            status="FAILED",
            durationMs=duration_ms,
            errorMessage=str(e),
        )
