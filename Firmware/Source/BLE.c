#include "BLE.h"
#include "UART.h"
#include "cmsis_os2.h"
#include "stdlib.h"
#include "string.h"
#include "utilities.h"

//UART_receive(rxBuffer, sizeof(rxBuffer));

//static uint8_t BLE_Flags=0;
osEventFlagsId_t BLE_Flags;


#pragma pack(1)
typedef struct
{
	uint8_t startByte;
	uint8_t length;
}PacketHeader;

typedef struct
{
	PacketHeader header;
	uint8_t data[0];
}PacketDef;

typedef union
{
	PacketDef pkt;
	uint8_t dataBuffer[PACKET_SIZE];
}Packet;

#pragma pack()

typedef struct
{
	Packet data;
}QueueElement;

typedef struct
{
	QueueElement entry[5];
}Queue;

static struct
{
  Packet txBuffer;
  //Packet rxBuffer;
	Queue rxQueue;
	uint8_t message_count;
} sBLE;

static BLE_moduleState BLE_stateOfModule;
static Packet txBuffer;
osThreadId_t tid_BLE;

static void BLE_handleUARTCB(UARTOperationEnum op);
static void BLE_Send(uint8_t *pData, uint32_t length);
static void BLE_Receive(uint8_t *pDest, uint32_t length);
static void BLE_enterCentral(void);
static void BLE_waitOnDevice(void);
static uint8_t BLE_BuildPacket(const char *pString);
static bool BLE_validatePacket(uint8_t queueEntryIndex);
void BLE_SendPacket(const char *pString);
void BLE_issueResetCommand(void);
static void BLE_stopReceiving(void);
static void BLE_handleReceiveFlag(void);
static bool BLE_searchForKeyword(uint8_t * keyword, uint8_t queueEntryIndex);

void BLE_init(void)
{
	sBLE.message_count=0;
	BLE_Flags = osEventFlagsNew(NULL);
  UART0_init(9600,&BLE_handleUARTCB);
	//BLE_Receive(sBLE.rxBuffer.dataBuffer,sizeof(sBLE.rxBuffer));
	//BLE_waitOnDevice();
#ifdef IS_HUB_DEVICE
	BLE_enterCentral();
#else
	
#endif
}

void Thread_BLE(void *arg)
{
	BLE_init();
	BLE_connectToDevice("20FABB049EBC");
	BLE_SendPacket("test packet");
	while(1)
	{
		uint32_t flagsToWaitFor = (BLE_MESSAGE_RECEIVED|BLE_MESSAGE_READY_FOR_TRANSMIT);
		uint32_t eventFlags = osEventFlagsWait(BLE_Flags,flagsToWaitFor,osFlagsWaitAny,1000);
		if(eventFlags & BLE_MESSAGE_RECEIVED)
		{
			BLE_handleReceiveFlag();
		}
		
		if(eventFlags & BLE_MESSAGE_READY_FOR_TRANSMIT)
		{
			//transmit whatever is in the BLE TX buffer
		}
		
		//TODO: process queue here is you want to instead
		//BLE_processQueue();
		
		
		//int8_t resp=255;
		//resp = BLE_stdCommand("VER");
		//Play_Recording(safe_female, 4376);
		//waits until recording complete, then clears flag
		//osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
	}
}

//This function is called in ISR context, so keep it short 
static void BLE_handleUARTCB(UARTOperationEnum op)
{
  switch(op)
  {
  case IO_ERROR:
    UART_Cancel();
    break;
  case IO_RECEIVE:
		//BLE_Flags |= BLE_receiveSuccessful;
		osEventFlagsSet(BLE_Flags, BLE_MESSAGE_RECEIVED);
    break;
	case IO_WRITE:
		osEventFlagsSet(BLE_Flags, BLE_TRANSMIT_SUCCESS);
    break;
  default:
    break;
  }
}

static void BLE_Send(uint8_t *pData, uint32_t length)
{
	UART_send(pData, length); //TODO: this should be getting passed the transmit buffer?
}

static void BLE_Receive(uint8_t *pDest, uint32_t length)
{
	UART_receive();
}

enum opResult BLE_Advertise(void)
{
	return SUCCESS;
}

enum opResult BLE_noAdvertise(void)
{
	return SUCCESS;
}

enum opResult BLE_stdCommand(uint8_t *command)
{
	//BLE_Receive(sBLE.rxBuffer.dataBuffer,sizeof(sBLE.rxBuffer));
	//Util_fillMemory(sBLE.rxBuffer.dataBuffer, sizeof(sBLE.rxBuffer), '\0'); //clear out Rx buffer between reads
	BLE_Send(command,strlen(command));
	BLE_Send("\r",1);
	uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED,osFlagsWaitAll,osWaitForever);
	UART_getPacket(sBLE.rxQueue.entry[0].data.dataBuffer,MESSAGE_SIZE); //add Message to Queue, entry 0
	while(!BLE_searchForKeyword("OK",0)); //validate response in queue, entry 0

	if(BLE_searchForKeyword("OK",0))
	{
		Util_fillMemory(sBLE.rxQueue.entry[0].data.dataBuffer, MESSAGE_SIZE, '\0');
		UART_resetRxBuffer();
		return SUCCESS;
	}
	else
	{
		Util_fillMemory(sBLE.rxQueue.entry[0].data.dataBuffer, MESSAGE_SIZE, '\0');
		UART_resetRxBuffer();
		return TIMEOUT_ERROR;
	}
}

static bool BLE_searchForKeyword(uint8_t * keyword, uint8_t queueEntryIndex)
{
	if(strstr(sBLE.rxQueue.entry[queueEntryIndex].data.dataBuffer,keyword)!=NULL)
	{
		return true;
	}
	else
	{
		return false;
	}
}

void BLE_enterCentral(void)
{
	BLE_stdCommand("set ACON=off"); //dont automatically connect
	BLE_stdCommand("set cent=on");
	BLE_stdCommand("wrt");
	BLE_issueResetCommand();
}

void BLE_connectToDevice(const char *pBTAddress)
{
	//BLE_Receive(sBLE.rxBuffer.dataBuffer,sizeof(sBLE.rxBuffer));
	BLE_Send("CON ",4);
	BLE_Send(pBTAddress,BLE_ADDRESS_LENGTH);
	BLE_Send(" 0\r",3);
	uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED,osFlagsWaitAll,osWaitForever);
	UART_getPacket(sBLE.rxQueue.entry[0].data.dataBuffer,MESSAGE_SIZE); //add Message to Queue, entry 0
	while(!BLE_searchForKeyword("CON=OK",0)); //validate response in queue, entry 0
	//BLE_stopReceiving();
}

void BLE_waitOnDevice(void)
{
	uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED,osFlagsWaitAll,osWaitForever);
	UART_getPacket(sBLE.rxQueue.entry[0].data.dataBuffer,MESSAGE_SIZE); //add Message to Queue, entry 0
	while(!BLE_searchForKeyword("READY",0)); //validate response in queue, entry 0
	//UART_Cancel();
}

void BLE_issueResetCommand(void)
{
	//BLE_Receive(sBLE.rxBuffer.dataBuffer,sizeof(sBLE.rxBuffer));
	BLE_Send("rst\r",4);
	BLE_waitOnDevice();
	//BLE_stopReceiving();
}

uint8_t BLE_BuildPacket(const char *pString)
{
	uint32_t dataLength = (uint8_t)strlen(pString);
	if(dataLength == 0 || dataLength > (PACKET_SIZE - sizeof(PacketHeader) - sizeof(uint16_t)))
	{
		return 0;
	}
	uint16_t dataCRC = Util_crc16(pString, dataLength);
	
	txBuffer.pkt.header.startByte = PACKET_START_BYTE;
	txBuffer.pkt.header.length = (uint8_t)dataLength;
	Util_copyMemory((uint8_t *)pString,txBuffer.pkt.data,dataLength);
	Util_copyMemory((uint8_t *)&dataCRC,&txBuffer.pkt.data[dataLength],sizeof(dataCRC));
	
	return (sizeof(PacketHeader) + dataLength + sizeof(dataCRC));
}

void BLE_SendPacket(const char *pString)
{
	uint8_t packetLength = BLE_BuildPacket(pString);
	if(packetLength > 0)
	{
		//BLE_Receive(sBLE.rxBuffer.dataBuffer,sizeof(sBLE.rxBuffer));
		BLE_Send("SND",3);
		BLE_Send(sBLE.txBuffer.dataBuffer,packetLength);
		BLE_Send("\r",1);
		//BLE_stopReceiving();
	}
}

bool BLE_validatePacket(uint8_t queueEntryIndex)
{
	//validate packet's CRC
	uint8_t receivedDataLength = sBLE.rxQueue.entry[queueEntryIndex].data.pkt.header.length;
	uint16_t crcValue = Util_crc16(sBLE.rxQueue.entry[queueEntryIndex].data.pkt.data, receivedDataLength);
	if(crcValue == *((uint16_t *)sBLE.rxQueue.entry[queueEntryIndex].data.dataBuffer) + receivedDataLength)
	{
		return true;
	}
	else
	{
		return false;
	}
}

static void BLE_stopReceiving(void)
{
	//UART_stop_receiving();
}

static void BLE_handleReceiveFlag(void)
{
	
	while(1)
	{
		
			//Check if queue is full, if so return
			//if(queueFull) return;
		
			uint32_t length = UART_getPacket()...
			if(length == 0)
				break;
		
		
	}
	
	
	uint8_t length = UART_getPacket(sBLE.rxQueue.entry[sBLE.message_count++].data.dataBuffer,MESSAGE_SIZE); //add Message to Queue
	for(uint8_t queueEntry=0;queueEntry<sBLE.message_count;queueEntry++) //analyze messages in queue
	{
		if(BLE_searchForKeyword("RCV=",queueEntry))
		{
			//TODO: 
			BLE_validatePacket(queueEntry);
			//send data from packet to App thread
			
		}
		else if(BLE_searchForKeyword("CON=OK",queueEntry))
		{
			//Change from "waiting to connection" to "connected" state
		}
		else if(BLE_searchForKeyword("OK",queueEntry))
		{
			//Change from "waiting on response" to "checking response" state
		}
		else if(BLE_searchForKeyword("ERR",queueEntry))
		{
			//Change from "waiting on response" to "checking response" state
		}
		else //throw out message
		{
			Util_fillMemory(sBLE.rxQueue.entry[sBLE.message_count--].data.dataBuffer, MESSAGE_SIZE, '\0');
		}
	}
}