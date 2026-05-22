from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()

class SMSData(BaseModel):
    sms: str
    sender: str
    time: str

@app.post("/transactions")
async def transactions(data: SMSData):
    print(data)
    return {"status": "received"}