#include "BLE.h"
#include "UART.h"
#include "cmsis_os2.h"
#include "stdlib.h"
#include "string.h"
#include "utilities.h"
#include "app.h"

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
	uint8_t data[PACKET_SIZE];
}QueueElement;

typedef struct
{
	QueueElement entry[5];
}Queue;

static struct
{
  Packet txBuffer;
  Packet rxPacket;
	Queue rxQueue;
	uint8_t message_count;
	BLE_moduleState BLE_currentModuleState;
} sBLE;

//static BLE_moduleState BLE_stateOfModule;
//static Packet txBuffer;
osThreadId_t tid_BLE;

static void BLE_handleUARTCB(UARTOperationEnum op);
static void BLE_Send(uint8_t *pData, uint32_t length);
static void BLE_enterCentral(void);
static void BLE_waitOnDevice(void);
static uint8_t BLE_BuildPacket(const char *pString);
static bool BLE_validatePacket(uint8_t queueEntryIndex);
void BLE_SendPacket(const char *pString);
void BLE_issueResetCommand(void);
static void BLE_handleReceiveFlag(void);
static bool BLE_searchForKeyword(uint8_t * keyword, uint8_t queueEntryIndex);

void BLE_init(void)
{
	sBLE.message_count=0;
	BLE_Flags = osEventFlagsNew(NULL);
  UART0_init(9600,&BLE_handleUARTCB);
#ifdef IS_HUB_DEVICE
	BLE_enterCentral();
#else
	
#endif
}

void Thread_BLE(void *arg)
{
	BLE_init();
	receivedMessageQ_id = osMessageQueueNew(1,sizeof(Packet),NULL);
	//BLE_connectToDevice("20FABB049EBC");
	//BLE_SendPacket("UNSAFE");
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
	sBLE.BLE_currentModuleState = SENDING_COMMAND;
	BLE_Send(command,strlen((const char*)command));
	BLE_Send("\r",1);
	sBLE.BLE_currentModuleState = AWAITING_RESPONSE;
	uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED,osFlagsWaitAll,osWaitForever);
	
	BLE_handleReceiveFlag();

	if(sBLE.BLE_currentModuleState == RESPONSE_RECEIVED)
	{
		sBLE.BLE_currentModuleState = IDLE;
		return SUCCESS;
	}
	else
	{
		return TIMEOUT_ERROR;
	}
}

static bool BLE_searchForKeyword(uint8_t * keyword, uint8_t queueEntryIndex)
{
	if(strstr((const char *)sBLE.rxQueue.entry[queueEntryIndex].data,(const char*)keyword)!=NULL)
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
	sBLE.BLE_currentModuleState = SENDING_COMMAND;
	BLE_Send("CON ",4);
	BLE_Send((uint8_t *)pBTAddress,BLE_ADDRESS_LENGTH);
	BLE_Send(" 0\r",3);
	sBLE.BLE_currentModuleState = AWAITING_RESPONSE;
	uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED,osFlagsWaitAll,osWaitForever);
	BLE_handleReceiveFlag();
}

void BLE_waitOnDevice(void)
{
	uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED,osFlagsWaitAll,osWaitForever);
	BLE_handleReceiveFlag();
}

void BLE_issueResetCommand(void)
{
	sBLE.BLE_currentModuleState = SENDING_COMMAND;
	uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED,osFlagsNoClear,0);
	BLE_Send("rst\r",4);
	sBLE.BLE_currentModuleState = AWAITING_RESPONSE;
	BLE_waitOnDevice();
	sBLE.BLE_currentModuleState = IDLE;
}

uint8_t BLE_BuildPacket(const char *pString)
{
	uint32_t dataLength = (uint8_t)strlen(pString);
	if(dataLength == 0 || dataLength > (PACKET_SIZE - sizeof(PacketHeader) - sizeof(uint16_t)))
	{
		return 0;
	}
	uint16_t dataCRC = Util_crc16(pString, dataLength);
	
	sBLE.txBuffer.pkt.header.startByte = PACKET_START_BYTE;
	sBLE.txBuffer.pkt.header.length = (uint8_t)dataLength;
	Util_copyMemory((uint8_t *)pString,sBLE.txBuffer.pkt.data,dataLength);
	Util_copyMemory((uint8_t *)&dataCRC,&sBLE.txBuffer.pkt.data[dataLength],sizeof(dataCRC));
	
	return (sizeof(PacketHeader) + dataLength + sizeof(dataCRC));
}

void BLE_SendPacket(const char *pString)
{
	uint8_t packetLength = BLE_BuildPacket(pString);
	if(packetLength > 0)
	{
		BLE_Send("SND ",4);
		BLE_Send(sBLE.txBuffer.dataBuffer,packetLength);
		BLE_Send("\r",1);
	}
}

bool BLE_validatePacket(uint8_t queueEntryIndex)
{
	//validate packet's CRC
	uint8_t receivedDataLength = sBLE.rxPacket.pkt.header.length;
	uint16_t crcValue = Util_crc16(sBLE.rxPacket.pkt.data, receivedDataLength);
	if(crcValue == *((uint16_t *)(sBLE.rxPacket.dataBuffer + receivedDataLength + 2)))
	{
		return true;
	}
	else
	{
		return false;
	}
}

static void BLE_handleReceiveFlag(void)
{
	//add messages in the UART buffer to the queue
	//process all messages in queue
	//clear queue

	osDelay(250); //wait for all possible response messages to finish coming in.
	osEventFlagsClear(BLE_Flags,BLE_MESSAGE_RECEIVED); //waiting for messages to come in caused flag to be set again. Clear to prevent prematurely entering later functions.
	while(1)
	{
		//TODO: Check if queue is full, if so return
		uint8_t length = UART_getPacket(sBLE.rxQueue.entry[sBLE.message_count].data);
		if(length == 0)
			break; //no message added to queue
		else
		{
			sBLE.message_count++;
		}
	}

	for(uint8_t queueEntry=0; queueEntry < sBLE.message_count; queueEntry++)
	{
		if(BLE_searchForKeyword("RCV=",queueEntry))
		{
			uint32_t testing = strlen((const char *)sBLE.rxQueue.entry[queueEntry].data)-8;
			Util_copyMemory(&sBLE.rxQueue.entry[queueEntry].data[4], sBLE.rxPacket.dataBuffer, strlen((const char *)sBLE.rxQueue.entry[queueEntry].data)-6); //-8 for RCV=\r\n
			if(BLE_validatePacket(queueEntry))
			{
				//TODO: send data from packet to App thread
				osMessageQueuePut(receivedMessageQ_id,&sBLE.rxPacket.pkt.data,NULL,1000); //send received message to app thread
				osEventFlagsWait(BLE_Flags,BLE_RECEIVED_MESSAGE_TRANSFERRED,osFlagsWaitAll,3000); //wait until received message has been copied over to app thread variable before deleting.
			}
		}
		else if(BLE_searchForKeyword("CON=OK",queueEntry))
		{
			//TODO: Change from "waiting to connection" to "connected" state
		}
		else if(BLE_searchForKeyword("OK",queueEntry))
		{
			//Change from "waiting on response" to "response received" state
			sBLE.BLE_currentModuleState = RESPONSE_RECEIVED;
		}
		else if(BLE_searchForKeyword("ERR",queueEntry))
		{
			//Change from "waiting on response" to "error" state
			sBLE.BLE_currentModuleState = ERROR_STATE;
		}
		else if(BLE_searchForKeyword("READY",queueEntry))
		{
			//Change from "waiting on response" to "response received" state
			sBLE.BLE_currentModuleState = RESPONSE_RECEIVED;
		}
		//once message has been processed, or if it was not something we care about, clear out that entry
		Util_fillMemory(sBLE.rxQueue.entry[queueEntry].data, MESSAGE_SIZE, '\0');
	}
	sBLE.message_count = 0; //all messages have been processed. Can begin adding to queue again
}