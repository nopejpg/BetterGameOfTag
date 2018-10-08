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
	QueueElement entry[10]; //queue size was 5. 
}Queue;

static struct
{
  Packet txBuffer;
  Packet rxPacket;
	Queue rxQueue;
	uint8_t message_count;
	BLE_moduleState BLE_currentModuleState;
	BLE_commState BLE_currentCommsState;
	bool deviceConnected;
} sBLE;

#ifdef IS_HUB_DEVICE
typedef struct
{
	const char Pod1_Address[12];
	const char Pod2_Address[12];
	const char Pod3_Address[12];
	bool Pod1_Connected;
	bool Pod2_Connected;
	bool Pod3_Connected;
	uint8_t Pod1_Current_State;
	uint8_t Pod2_Current_State;
	uint8_t Pod3_Current_State;
	bool Pod1_Detected;
	bool Pod2_Detected;
	bool Pod3_Detected;
}podInfo;

podInfo podInfoList = {.Pod1_Address = "20FABB049EA5",.Pod2_Address = "20FABB049E77",
											.Pod1_Connected = false,.Pod2_Connected = false,.Pod3_Connected = false,
											.Pod1_Current_State = INIT, .Pod2_Current_State = INIT, .Pod3_Current_State = INIT,
											.Pod1_Detected = false, .Pod2_Detected = false, .Pod3_Detected = false};

static char phoneAddress[12]; //phone address should be a 12 byte hex string
static char scannedAddress[12];
#endif //IS_HUB_DEVICE

//static BLE_moduleState BLE_stateOfModule;
//static Packet txBuffer;
osThreadId_t tid_BLE;

static void BLE_handleUARTCB(UARTOperationEnum op);
static void BLE_Send(uint8_t *pData, uint32_t length);
static bool BLE_searchForKeyword(uint8_t * keyword, uint8_t queueEntryIndex);
static void BLE_issueResetCommand(void);
static uint8_t BLE_BuildPacket(const char *pString);
static void BLE_SendPacket(uint32_t packetLength);
static bool BLE_validatePacket(uint8_t queueEntryIndex);
static void BLE_handleReceiveFlag(void);
static void BLE_SendCommand(const char *pString);
static void BLE_SendAck(void);
static void BLE_waitOnDevice(void);

#ifdef IS_HUB_DEVICE
static void BLE_enterCentral(void);
static bool BLE_connectToDevice(const char *pBTAddress);
static void BLE_connectToPod(void);
static void BLE_findPhoneAddress(void);
static void BLE_waitForPods(void);
static void BLE_connectToPhone(void);
static void BLE_changePodStates(void);
static void BLE_disconnectFromDevice(void);
#endif //IS_HUB_DEVICE


void BLE_init(void)
{
	sBLE.message_count=0;
	sBLE.deviceConnected = false;
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
	deviceConnectionRequestQ_id = osMessageQueueNew(1,sizeof(POD1),NULL);
	requestedPodStatesQ_id = osMessageQueueNew(1,3*sizeof(uint8_t),NULL);
	while(1)
	{
		osThreadFlagsSet(tid_APP,BLE_INIT_AND_CONNECTED); //TESTING
		uint32_t flagsToWaitFor = (BLE_MESSAGE_RECEIVED | BLE_MESSAGE_READY_FOR_TRANSMIT | APP_THREAD_REQESTING_ACTION);
		uint32_t eventFlags = osEventFlagsWait(BLE_Flags,flagsToWaitFor,osFlagsWaitAny,1000);
		if(eventFlags != 0xFFFFFFFE) //if we didn't time out
		{
			if(eventFlags & BLE_MESSAGE_RECEIVED)
			{
				BLE_handleReceiveFlag();
			}
			if(eventFlags & APP_THREAD_REQESTING_ACTION)
			{
				flagsToWaitFor = (APP_CONNECT_TO_POD);
				uint32_t appRequestEventFlags = osEventFlagsGet(APP_Request_Flags);
				if(appRequestEventFlags != 0xFFFFFFFE) //if we didn't time out
				{
					#ifdef IS_HUB_DEVICE
					if(appRequestEventFlags & APP_CONNECT_TO_POD)
					{
						BLE_connectToPod();
					}
					if(appRequestEventFlags & APP_FIND_PHONE_ADDRESS)
					{
						BLE_findPhoneAddress();
					}
					if(appRequestEventFlags & APP_WAIT_FOR_PODS)
					{
						BLE_waitForPods();
					}
					if(appRequestEventFlags & APP_CONNECT_TO_PHONE)
					{
						BLE_connectToPhone();
					}
					if(appRequestEventFlags & APP_CHANGE_POD_STATES)
					{
						BLE_changePodStates();
					}
					#endif //IS_HUB_DEVICE
					if(appRequestEventFlags & APP_SEND_ACK)
					{
						BLE_SendAck();
					}
					osEventFlagsSet(APP_Request_Flags,APP_REQUEST_COMPLETE); //tell APP thread we are done processing its request
				}
			}
			
			
			if(eventFlags & BLE_MESSAGE_READY_FOR_TRANSMIT) //TODO: should this be used? (BLE_SendPacket puts data in txbuffer, and sets flag. Then this function sends out the data)
			{
				//transmit whatever is in the BLE TX buffer
			}
		}
		//TODO: process queue here is you want to instead
		//BLE_processQueue();
	}
}

//This function is called in ISR context, so keep it short 
static void BLE_handleUARTCB(UARTOperationEnum op)
{
  switch(op)
  {
  case IO_ERROR:
    //UART_Cancel(); //TODO: Handle errors in future. For now, rely on retries and focus on functionality.
		UART_resetRxBuffer();
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

enum opResult BLE_stdCommand(uint8_t *command)
{
	sBLE.BLE_currentModuleState = SENDING_COMMAND;
	BLE_Send(command,strlen((const char*)command));
	BLE_Send((uint8_t *)"\r",1);
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

static void BLE_issueResetCommand(void)
{
	sBLE.BLE_currentModuleState = SENDING_COMMAND;
	uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED,osFlagsNoClear,0);
	BLE_Send((uint8_t *)"rst\r",4);
	sBLE.BLE_currentModuleState = AWAITING_RESPONSE;
	BLE_waitOnDevice();
	sBLE.BLE_currentModuleState = IDLE;
}

static uint8_t BLE_BuildPacket(const char *pString)
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

static void BLE_SendPacket(uint32_t packetLength)
{
	BLE_Send((uint8_t *)"SND ",4);
	BLE_Send(sBLE.txBuffer.dataBuffer,packetLength);
	BLE_Send((uint8_t *)"\r",1);
	//Util_fillMemory(sBLE.txBuffer.dataBuffer,packetLength,'\0'); //if we clear TX here, retries dont work
}

static bool BLE_validatePacket(uint8_t queueEntryIndex)
{
	//validate packet's CRC
	uint8_t receivedDataLength = sBLE.rxPacket.pkt.header.length;
	uint16_t crcValue = Util_crc16(sBLE.rxPacket.pkt.data, receivedDataLength);
	uint8_t received_CRC_b1 = *(sBLE.rxPacket.dataBuffer + receivedDataLength + 2);
	uint8_t received_CRC_b2 = *(sBLE.rxPacket.dataBuffer + receivedDataLength + 3);
	uint16_t receivedCRC_full = received_CRC_b1|(received_CRC_b2 << 8);
	if(crcValue == receivedCRC_full)
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
			if(sBLE.message_count > 10)
			{
				asm("nop"); //we need to increase the amount of entries in the queue
			}
		}
	}

	for(uint8_t queueEntry=0; queueEntry < sBLE.message_count; queueEntry++)
	{
		if(BLE_searchForKeyword((uint8_t *)"RCV=",queueEntry))
		{
			
		#ifdef IS_HUB_DEVICE //hub device is only one who should be receiving acks. If this is a hub device, check to see if we get an ack.
			if(BLE_searchForKeyword((uint8_t *)"ACK",queueEntry)) //if we get an ack
			{
				sBLE.BLE_currentCommsState = ACK_RECEIVED;
				osEventFlagsSet(BLE_Flags,BLE_ACK_RECEIVED); //signal that ack has been received, and app thread can start running again
				break; //don't go on to try and give app task the received data. This data was for us.
			}
		#endif
			Util_copyMemory(&sBLE.rxQueue.entry[queueEntry].data[4], sBLE.rxPacket.dataBuffer, strlen((const char *)sBLE.rxQueue.entry[queueEntry].data)-6); //-6 for RCV=\r\n
			if(BLE_validatePacket(queueEntry))
			{
				osMessageQueuePut(receivedMessageQ_id,&sBLE.rxPacket.pkt.data,NULL,1000); //send received message to app thread
				osEventFlagsWait(BLE_Flags,BLE_RECEIVED_MESSAGE_TRANSFERRED,osFlagsWaitAll,3000); //wait until received message has been copied over to app thread variable before deleting.
				osMessageQueueReset(receivedMessageQ_id); //TESTING. DOES THIS PREVENT REPEAT MESSAGES?
			}
		}
		//else if(BLE_searchForKeyword((uint8_t *)"FTRS",queueEntry)) //a string the BLE sends when it is connected successfully
		else if(BLE_searchForKeyword((uint8_t *)"DCFG=NOT",queueEntry)) //a string the BLE sends when it is connected successfully
		{
			//Change from "waiting to connection" to "connected" state
			sBLE.BLE_currentCommsState = CONNECTED_IDLE;
			sBLE.BLE_currentModuleState = RESPONSE_RECEIVED;
			osEventFlagsSet(BLE_Flags,BLE_CONNECTED_TO_DEVICE); //signals that a requested connection to a device was successfully made
		}
		else if(BLE_searchForKeyword((uint8_t *)"OK",queueEntry))
		{
			//Change from "waiting on response" to "response received" state
			sBLE.BLE_currentModuleState = RESPONSE_RECEIVED;
		}
		else if(BLE_searchForKeyword((uint8_t *)"ERR",queueEntry))
		{
			//Change from "waiting on response" to "error" state
			sBLE.BLE_currentModuleState = ERROR_STATE;
		}
		else if(BLE_searchForKeyword((uint8_t *)"READY",queueEntry))
		{
			//Change from "waiting on response" to "response received" state
			sBLE.BLE_currentModuleState = RESPONSE_RECEIVED;
		}
		#ifdef IS_HUB_DEVICE
		else if(BLE_searchForKeyword((uint8_t *)"SCN=",queueEntry)) //send scan results over to app thread so that it can find the phone's bluetooth address
		{
			if(sBLE.rxQueue.entry[queueEntry].data[30] == '1') //1 in this position (after the M) means it is melodysmart enabled
			{
				Util_copyMemory(&sBLE.rxQueue.entry[queueEntry].data[6],(uint8_t *)scannedAddress,12);
				if(strncmp(scannedAddress,podInfoList.Pod1_Address,12) == 0)
					podInfoList.Pod1_Detected = true;
				else if(strncmp(scannedAddress,podInfoList.Pod2_Address,12) == 0)
					podInfoList.Pod2_Detected = true;
				else if(strncmp(scannedAddress,podInfoList.Pod3_Address,12) == 0)
					podInfoList.Pod3_Detected = true;
				osEventFlagsSet(BLE_Flags,BLE_MELODYSMART_ADDRESS_FOUND);
			}
		}
		#endif //IS_HUB_DEVICE
		else if(BLE_searchForKeyword((uint8_t *)"STS=",queueEntry))
		{
			//Change from "waiting on response" to "response received" state
			sBLE.BLE_currentModuleState = RESPONSE_RECEIVED;
		}
		//once message has been processed, or if it was not something we care about, clear out that entry
		Util_fillMemory(sBLE.rxQueue.entry[queueEntry].data, MESSAGE_SIZE, '\0');
	}
	sBLE.message_count = 0; //all messages have been processed. Can begin adding to queue again
}

static void BLE_SendCommand(const char *pString) //this will be called from the app_task context!
{
	//this function will use BLE_SendPacket, mixed with retries, to help ensure faithful communication to other bluetooth device.
	//Only the hub will be using this function (to send instructions to pod devices). The pod devices will send back ACKs using the BLE_SendAck function.
	uint32_t flags;
	uint8_t packetLength = BLE_BuildPacket(pString);
	if(packetLength > 0)
	{
		//send packet
		//when ack is received, state will change
		for(uint8_t retries=0; retries < COMMS_MAX_RETRIES; retries++)
		{
			sBLE.BLE_currentCommsState = SENDING_PACKET;
			BLE_SendPacket(packetLength);
			sBLE.BLE_currentCommsState = AWAITING_ACK;
			flags = osEventFlagsWait(BLE_Flags,BLE_ACK_RECEIVED,osFlagsWaitAll,3000); //suspend app thread execution until ack is received
			if(flags & BLE_ACK_RECEIVED)
			{
				sBLE.BLE_currentCommsState = ACK_RECEIVED;
				break; //no need to retry
			}
		}
	}
}

static void BLE_SendAck(void)
{
	uint8_t packetLength = BLE_BuildPacket("ACK");
	BLE_SendPacket(packetLength);
	osEventFlagsClear(APP_Request_Flags,APP_SEND_ACK); //clear APP_SEND_ACK flag
}

static void BLE_waitOnDevice(void)
{
	uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED,osFlagsWaitAll,osWaitForever);
	BLE_handleReceiveFlag();
}

#ifdef IS_HUB_DEVICE
static void BLE_enterCentral(void)
{
	int32_t result;
	//first get device back to factory settings
	do
	{
		result = BLE_stdCommand((uint8_t *)"rtr");
	}while(result != SUCCESS);
	do
	{
		result = BLE_stdCommand((uint8_t *)"wrt");
	}while(result != SUCCESS);
	BLE_issueResetCommand();
	
	//go into central mode
	do
	{
		result = BLE_stdCommand((uint8_t *)"set ACON=off"); //dont automatically connect
	}while(result != SUCCESS);
	do
	{
		result = BLE_stdCommand((uint8_t *)"set cent=on");
	}while(result != SUCCESS);
	do
	{
		result = BLE_stdCommand((uint8_t *)"set scnp=000F4240 00002BF2"); //set scan interval to 1000000us (0x000F4240) to prevent UART from being overwhelmed
	}while(result != SUCCESS);
	do
	{
		result = BLE_stdCommand((uint8_t *)"wrt");
	}while(result != SUCCESS);
	BLE_issueResetCommand();
}

static bool BLE_connectToDevice(const char *pBTAddress)
{
	sBLE.BLE_currentModuleState = SENDING_COMMAND;
	BLE_Send((uint8_t *)"CON ",4);
	BLE_Send((uint8_t *)pBTAddress,BLE_ADDRESS_LENGTH);
	BLE_Send((uint8_t *)" 0\r",3);
	sBLE.BLE_currentModuleState = AWAITING_RESPONSE;

	osDelay(500); //Give time for all messages to come in so we can process them all at once
	uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED,osFlagsWaitAll,osWaitForever);
	BLE_handleReceiveFlag();
	
	events = osEventFlagsGet(BLE_Flags);
	
	if((events&BLE_CONNECTED_TO_DEVICE)&&(events != 0xFFFFFFFE))
	{
		osEventFlagsClear(BLE_Flags, BLE_CONNECTED_TO_DEVICE); //need to clear flag manually since we did not "wait" on it
		sBLE.BLE_currentCommsState = CONNECTED_IDLE;
		sBLE.deviceConnected = true;
		return true;
	}
	else
	{
		return false;
	}
}

static void BLE_connectToPod(void)
{
	//check queues, connect to pod here, etc
	uint32_t podToConnectTo;
	uint32_t queueResult = osMessageQueueGet(deviceConnectionRequestQ_id, &podToConnectTo, NULL, 1000); //wait until command from Phone App is received
	bool connectionEstablished=false;
	if(queueResult == osOK)
	{
		if(podToConnectTo == POD1)
		{
			connectionEstablished = BLE_connectToDevice(podInfoList.Pod1_Address);
		}
		else if(podToConnectTo == POD2)
		{
			connectionEstablished = BLE_connectToDevice(podInfoList.Pod2_Address);
		}
		else if(podToConnectTo == POD3)
		{
			connectionEstablished = BLE_connectToDevice(podInfoList.Pod3_Address);
		}
	}
	osEventFlagsClear(APP_Request_Flags,APP_CONNECT_TO_POD); //clear APP_CONNECT_TO_POD flag
	osMessageQueuePut(deviceConnectionRequestQ_id,&connectionEstablished,NULL,1000); //send result back to APP thread
}

static void BLE_findPhoneAddress(void)
{
	while(1)
	{
		uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED,osFlagsWaitAll,osWaitForever);
		BLE_handleReceiveFlag();
		
		events = osEventFlagsGet(BLE_Flags);
		if(events & BLE_MELODYSMART_ADDRESS_FOUND)
		{
			osEventFlagsClear(BLE_Flags, BLE_MELODYSMART_ADDRESS_FOUND); //clear BLE_MELODYSMART_ADDRESS_FOUND flag until next address is found
			if((strncmp(scannedAddress, podInfoList.Pod1_Address, 12)!= 0)&&(strncmp(scannedAddress, podInfoList.Pod2_Address, 12)!= 0)&&(strncmp(scannedAddress, podInfoList.Pod3_Address, 12)!= 0))
			{
				Util_copyMemory((uint8_t *)scannedAddress,(uint8_t *)phoneAddress,12); //phone address is unidentified address that supports melodysmart protocol
				osEventFlagsClear(APP_Request_Flags, APP_FIND_PHONE_ADDRESS); //clear APP_FIND_PHONE_ADDRESS flag
				break;
			}
		}
	}
	//disable scanning here
}

static void BLE_waitForPods(void)
{
	//while((podInfoList.Pod1_Detected == false)||(podInfoList.Pod2_Detected == false)||(podInfoList.Pod3_Detected == false))
	//while(podInfoList.Pod1_Detected == false) //TODO: REMOVE AND REPLACE WITH ABOVE CODE. THIS IS FOR TESTING ONLY.
	while((podInfoList.Pod1_Detected == false)||(podInfoList.Pod2_Detected == false)) //TODO: REMOVE AND REPLACE WITH ABOVE CODE (w/ all 3 pods). THIS IS FOR TESTING ONLY.
	{
		uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED,osFlagsWaitAll,osWaitForever);
		BLE_handleReceiveFlag();
	}
	osEventFlagsClear(APP_Request_Flags, APP_WAIT_FOR_PODS); //clear APP_WAIT_FOR_PODS flag
}

static void BLE_connectToPhone(void)
{
	bool connectionEstablished;
	do
	{
		connectionEstablished = BLE_connectToDevice(phoneAddress);
	}while(connectionEstablished == false);
	osEventFlagsClear(APP_Request_Flags,APP_CONNECT_TO_PHONE); //clear APP_CONNECT_TO_POD flag
	osMessageQueuePut(deviceConnectionRequestQ_id,&connectionEstablished,NULL,1000); //send result back to APP thread
}

static void BLE_changePodStates(void)
{
	uint8_t requestedPodStates[3];
	uint32_t testingResult = osMessageQueueGet(requestedPodStatesQ_id,&requestedPodStates,NULL,1000);
	bool connectionResult;
	if(sBLE.deviceConnected)
		BLE_disconnectFromDevice();
	if(podInfoList.Pod1_Current_State != requestedPodStates[0])
	{
		podInfoList.Pod1_Current_State = requestedPodStates[0];
		do
		{
			connectionResult = BLE_connectToDevice(podInfoList.Pod1_Address);
		}while(connectionResult == false);
		if(requestedPodStates[0]==SAFE)
			BLE_SendCommand("SAFE");
		else if(requestedPodStates[0]==UNSAFE)
			BLE_SendCommand("UNSAFE");
		BLE_disconnectFromDevice();
	}
	if(podInfoList.Pod2_Current_State != requestedPodStates[1])
	{
		podInfoList.Pod2_Current_State = requestedPodStates[1];
		do
		{
			connectionResult = BLE_connectToDevice(podInfoList.Pod2_Address);
		}while(connectionResult == false);
		if(requestedPodStates[1]==SAFE)
			BLE_SendCommand("SAFE");
		else if(requestedPodStates[1]==UNSAFE)
			BLE_SendCommand("UNSAFE");
		BLE_disconnectFromDevice();
	}
//	if(podInfoList.Pod3_Current_State != requestedPodStates[2])
//	{
//		podInfoList.Pod3_Current_State = requestedPodStates[2];
//		do
//		{
//			connectionResult = BLE_connectToDevice(podInfoList.Pod3_Address);
//		}while(connectionResult == false);
//		if(requestedPodStates[2]==SAFE)
//			BLE_SendCommand("SAFE");
//		else if(requestedPodStates[2]==UNSAFE)
//			BLE_SendCommand("UNSAFE");
//		BLE_disconnectFromDevice();
//	}
	BLE_connectToPhone();
	osEventFlagsClear(APP_Request_Flags,APP_CHANGE_POD_STATES); //clear APP_CONNECT_TO_POD flag
}

static void BLE_disconnectFromDevice(void)
{
	BLE_stdCommand((uint8_t *)"DCN");
	sBLE.deviceConnected = false;
}


#endif //IS_HUB_DEVICE


