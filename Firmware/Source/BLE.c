#include "BLE.h"
#include "UART.h"
#include "cmsis_os2.h"
#include "stdlib.h"
#include "string.h"
#include "utilities.h"
#include "app.h"

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
	Queue rxQueue_fromPhone;
	Queue rxQueue_fromPods;
	uint8_t message_count_fromPhone;
	uint8_t message_count_fromPods;
	BLE_moduleState BLE_currentModuleState_forPhone;
	BLE_moduleState BLE_currentModuleState_forPods;
	BLE_commState BLE_currentCommsState_forPhone;
	BLE_commState BLE_currentCommsState_forPods;
	bool phoneConnected;
	bool podConnected;
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
	bool Pod1_Online;
	bool Pod2_Online;
	bool Pod3_Online;
}podInfo;

podInfo podInfoList = {.Pod1_Address = "20FABB049EA5",.Pod2_Address = "20FABB049E77", .Pod3_Address = "20FABB049E7B",
											.Pod1_Connected = false,.Pod2_Connected = false,.Pod3_Connected = false,
											.Pod1_Current_State = INIT, .Pod2_Current_State = INIT, .Pod3_Current_State = INIT,
											.Pod1_Online = false, .Pod2_Online = false, .Pod3_Online = false};

static char scannedAddress[12];
static char connectedDeviceAddress[12];
#endif //IS_HUB_DEVICE

osThreadId_t tid_BLE;
static void BLE_handleUARTCB_forPhone(UARTOperationEnum op);											
static void BLE_Send(uint8_t *pData, uint32_t length, uint8_t UART_num);
static bool BLE_searchForKeyword(uint8_t * keyword, uint8_t queueEntryIndex, Queue * rxQueue);
static void BLE_issueResetCommand(uint8_t BLE_module);
static uint8_t BLE_BuildPacket(const char *pString);
static void BLE_SendPacket(uint32_t packetLength, uint8_t BLE_module);
static bool BLE_validatePacket(void);
static void BLE_handleReceiveFlag_fromPhone(void); //used by pod to handle messages from hub	
static void BLE_SendAck(void);
static void BLE_waitOnDevice(uint8_t BLE_module);
static void BLE_initBLE_forPhone(void); //used by pod to init its BLE

//static void BLE_tempStopConAttempts(uint32_t time_ms); //dont need this function anymore
#ifdef IS_HUB_DEVICE
static void BLE_handleUARTCB_forPods(UARTOperationEnum op);
static bool BLE_connectToDevice(const char *pBTAddress);
static bool BLE_isDeviceConnected(void);
static void BLE_waitForPods(void);
static void BLE_changePodStates(void);
static void BLE_disconnectFromDevice(void);
static void BLE_initBLE_forPods(void);
static void BLE_handleReceiveFlag_fromPods(void);	
static void BLE_SendCommand(const char *pString);
#endif //IS_HUB_DEVICE


void BLE_init(void)
{
	sBLE.message_count_fromPhone=0;
	sBLE.message_count_fromPods=0;
	sBLE.phoneConnected = false;
	sBLE.podConnected = false;
	BLE_Flags = osEventFlagsNew(NULL);
  UART0_init(9600,&BLE_handleUARTCB_forPhone);
	#ifdef IS_HUB_DEVICE
	UART1_init(9600,&BLE_handleUARTCB_forPods);
	BLE_initBLE_forPods();
	#endif //IS_HUB_DEVICE
	BLE_initBLE_forPhone();
}

void Thread_BLE(void *arg)
{
	BLE_init();
	receivedMessageQ_id = osMessageQueueNew(1,sizeof(Packet),NULL);
	deviceConnectionRequestQ_id = osMessageQueueNew(1,sizeof(POD1),NULL);
	requestedPodStatesQ_id = osMessageQueueNew(1,3*sizeof(uint8_t),NULL);
	podStateRequestResultsQ_id = osMessageQueueNew(1,3*sizeof(bool),NULL);
	while(1)
	{
		osThreadFlagsSet(tid_APP,BLE_INIT_AND_CONNECTED);
		uint32_t flagsToWaitFor = (BLE_MESSAGE_RECEIVED_FROM_PHONE | BLE_MESSAGE_RECEIVED_FROM_PODS | BLE_MESSAGE_READY_FOR_TRANSMIT | APP_THREAD_REQESTING_ACTION | BLE_TEMP_STOP_CON_ATTEMPTS);
		uint32_t eventFlags = osEventFlagsWait(BLE_Flags,flagsToWaitFor,osFlagsWaitAny,1000);
		if(eventFlags != 0xFFFFFFFE) //if we didn't time out
		{
			if(eventFlags & BLE_MESSAGE_RECEIVED_FROM_PHONE)
			{
				BLE_handleReceiveFlag_fromPhone();
			}
			#ifdef IS_HUB_DEVICE
			if(eventFlags & BLE_MESSAGE_RECEIVED_FROM_PODS)
			{
				BLE_handleReceiveFlag_fromPods();
			}
			#endif //IS_HUB_DEVICE
			if(eventFlags & APP_THREAD_REQESTING_ACTION)
			{
				//flagsToWaitFor = (APP_CONNECT_TO_POD);
				uint32_t appRequestEventFlags = osEventFlagsGet(APP_Request_Flags);
				if(appRequestEventFlags != 0xFFFFFFFE) //if we didn't time out
				{
					#ifdef IS_HUB_DEVICE
					if(appRequestEventFlags & APP_WAIT_FOR_PODS)
					{
						BLE_waitForPods();
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
static void BLE_handleUARTCB_forPhone(UARTOperationEnum op)
{
  switch(op)
  {
  case IO_ERROR:
    //UART_Cancel(); //TODO: Handle errors in future. For now, rely on retries and focus on functionality.
		UART_resetRxBuffer();
    break;
  case IO_RECEIVE:
		osEventFlagsSet(BLE_Flags, BLE_MESSAGE_RECEIVED_FROM_PHONE);
    break;
	case IO_WRITE:
		osEventFlagsSet(BLE_Flags, BLE_TRANSMIT_SUCCESS);
    break;
  default:
    break;
  }
}

#ifdef IS_HUB_DEVICE
static void BLE_handleUARTCB_forPods(UARTOperationEnum op)
{
  switch(op)
  {
  case IO_ERROR:
    //UART_Cancel(); //TODO: Handle errors in future. For now, rely on retries and focus on functionality.
		UART_resetRxBuffer();
    break;
  case IO_RECEIVE:
		osEventFlagsSet(BLE_Flags, BLE_MESSAGE_RECEIVED_FROM_PODS);
    break;
	case IO_WRITE:
		osEventFlagsSet(BLE_Flags, BLE_TRANSMIT_SUCCESS);
    break;
  default:
    break;
  }
}
#endif //IS_HUB_DEVICE

static void BLE_Send(uint8_t *pData, uint32_t length, uint8_t UART_num)
{
	UART_send(pData, length, UART_num); //TODO: this should be getting passed the transmit buffer?
}

enum opResult BLE_stdCommand(uint8_t *command, uint8_t UART_num)
{
	if(UART_num == SEND_TO_PHONE)
		sBLE.BLE_currentModuleState_forPhone = SENDING_COMMAND;
	else
		sBLE.BLE_currentModuleState_forPods = SENDING_COMMAND;
	BLE_Send(command,strlen((const char*)command), UART_num);
	BLE_Send((uint8_t *)"\r",1, UART_num);
	if(UART_num == SEND_TO_PHONE)
		sBLE.BLE_currentModuleState_forPhone = AWAITING_RESPONSE;
	else
		sBLE.BLE_currentModuleState_forPods = AWAITING_RESPONSE;
	if(UART_num == SEND_TO_PHONE)
	{
		osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED_FROM_PHONE,osFlagsWaitAll,osWaitForever);
		BLE_handleReceiveFlag_fromPhone();
	}
	#ifdef IS_HUB_DEVICE
	else
	{
		osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED_FROM_PODS,osFlagsWaitAll,osWaitForever);
		BLE_handleReceiveFlag_fromPods();
	}
	#endif //IS_HUB_DEVICE
	
	if((sBLE.BLE_currentModuleState_forPhone == RESPONSE_RECEIVED)&&(UART_num == SEND_TO_PHONE))
	{
		sBLE.BLE_currentModuleState_forPhone = IDLE;
		return SUCCESS;
	}
	else if((sBLE.BLE_currentModuleState_forPods == RESPONSE_RECEIVED)&&(UART_num == SEND_TO_POD))
	{
		sBLE.BLE_currentModuleState_forPods = IDLE;
		return SUCCESS;
	}
	else
	{
		return TIMEOUT_ERROR;
	}
}

static bool BLE_searchForKeyword(uint8_t * keyword, uint8_t queueEntryIndex, Queue * rxQueue)
{
	
	//if(strstr((const char *)sBLE.rxQueue.entry[queueEntryIndex].data,(const char*)keyword)!=NULL)
	if(strstr((const char *)rxQueue->entry[queueEntryIndex].data,(const char*)keyword)!=NULL)
	{
		return true;
	}
	else
	{
		return false;
	}
}

static void BLE_issueResetCommand(uint8_t BLE_module)
{
	if(BLE_module == PHONE_BLE)
		sBLE.BLE_currentModuleState_forPhone = SENDING_COMMAND;
	else
		sBLE.BLE_currentModuleState_forPods = SENDING_COMMAND;
	if(BLE_module == PHONE_BLE)
		osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED_FROM_PHONE,osFlagsNoClear,0);
	else
		osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED_FROM_PODS,osFlagsNoClear,0);
	BLE_Send((uint8_t *)"rst\r",4,BLE_module);
	if(BLE_module == PHONE_BLE)
		sBLE.BLE_currentModuleState_forPhone = AWAITING_RESPONSE;
	else
		sBLE.BLE_currentModuleState_forPods = AWAITING_RESPONSE;
	BLE_waitOnDevice(BLE_module);
	if(BLE_module == PHONE_BLE)
		sBLE.BLE_currentModuleState_forPhone = IDLE;
	else
		sBLE.BLE_currentModuleState_forPods = IDLE;
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

static void BLE_SendPacket(uint32_t packetLength, uint8_t BLE_module)
{
	BLE_Send((uint8_t *)"SND ", 4, BLE_module);
	BLE_Send(sBLE.txBuffer.dataBuffer,packetLength, BLE_module);
	BLE_Send((uint8_t *)"\r", 1, BLE_module);
}

static bool BLE_validatePacket(void)
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

static void BLE_handleReceiveFlag_fromPhone(void)
{
	//add messages in the UART buffer to the queue
	//process all messages in queue
	//clear queue

	osDelay(250); //wait for all possible response messages to finish coming in.
	osEventFlagsClear(BLE_Flags,BLE_MESSAGE_RECEIVED_FROM_PHONE); //waiting for messages to come in caused flag to be set again. Clear to prevent prematurely entering later functions.
	while(1)
	{
		//TODO: Check if queue is full, if so return
		uint8_t length = UART_getPacket_fromPhone(sBLE.rxQueue_fromPhone.entry[sBLE.message_count_fromPhone].data);
		if(length == 0)
			break; //no message added to queue
		else
		{
			sBLE.message_count_fromPhone++;
		}
	}

	for(uint8_t queueEntry=0; queueEntry < sBLE.message_count_fromPhone; queueEntry++)
	{
		if(BLE_searchForKeyword((uint8_t *)"RCV=", queueEntry, &sBLE.rxQueue_fromPhone))
		{
			
		#ifdef IS_HUB_DEVICE //hub device is only one who should be receiving acks. If this is a hub device, check to see if we get an ack.
			if(BLE_searchForKeyword((uint8_t *)"ACK", queueEntry, &sBLE.rxQueue_fromPhone)) //if we get an ack
			{
				sBLE.BLE_currentCommsState_forPhone = ACK_RECEIVED;
				osEventFlagsSet(BLE_Flags,BLE_ACK_RECEIVED); //signal that ack has been received, and app thread can start running again
				break; //don't go on to try and give app task the received data. This data was for us.
			}
		#endif
			Util_copyMemory(&sBLE.rxQueue_fromPhone.entry[queueEntry].data[4], sBLE.rxPacket.dataBuffer, strlen((const char *)sBLE.rxQueue_fromPhone.entry[queueEntry].data)-6); //-6 for RCV=\r\n
			if(BLE_validatePacket())
			{
				osEventFlagsSet(APP_Request_Flags, APP_MESSAGE_PENDING_FROM_BLE);
				osMessageQueuePut(receivedMessageQ_id,&sBLE.rxPacket.pkt.data,NULL,1000); //send received message to app thread
				osEventFlagsWait(BLE_Flags,BLE_RECEIVED_MESSAGE_TRANSFERRED,osFlagsWaitAll,3000); //wait until received message has been copied over to app thread variable before deleting.
				osMessageQueueReset(receivedMessageQ_id); //TESTING. DOES THIS PREVENT REPEAT MESSAGES?
			}
		}
		else if(BLE_searchForKeyword((uint8_t *)"OK", queueEntry, &sBLE.rxQueue_fromPhone))
		{
			//Change from "waiting on response" to "response received" state
			sBLE.BLE_currentModuleState_forPhone = RESPONSE_RECEIVED;
		}
		else if(BLE_searchForKeyword((uint8_t *)"ERR", queueEntry, &sBLE.rxQueue_fromPhone))
		{
			//Change from "waiting on response" to "error" state
			sBLE.BLE_currentModuleState_forPhone = ERROR_STATE;
		}
		else if(BLE_searchForKeyword((uint8_t *)"READY", queueEntry, &sBLE.rxQueue_fromPhone))
		{
			//Change from "waiting on response" to "response received" state
			sBLE.BLE_currentModuleState_forPhone = RESPONSE_RECEIVED;
		}
		
		#ifdef IS_HUB_DEVICE
		else if(BLE_searchForKeyword((uint8_t *)"STS=C CON", queueEntry, &sBLE.rxQueue_fromPhone)) //we are successfully connected
		{
			//Change from "waiting to connection" to "connected" state
			sBLE.BLE_currentCommsState_forPhone = CONNECTED_IDLE;
			sBLE.BLE_currentModuleState_forPhone = RESPONSE_RECEIVED;
			Util_copyMemory(&sBLE.rxQueue_fromPhone.entry[queueEntry].data[10],(uint8_t *)connectedDeviceAddress,12);
			osEventFlagsSet(BLE_Flags,BLE_CONNECTED_TO_PHONE); //signals that a requested connection to a device was successfully made
			
			
			//Change from "waiting on response" to "response received" state
			sBLE.BLE_currentModuleState_forPhone = RESPONSE_RECEIVED;
		}
		#endif //IS_HUB_DEVICE
		
		//once message has been processed, or if it was not something we care about, clear out that entry
		Util_fillMemory(sBLE.rxQueue_fromPhone.entry[queueEntry].data, MESSAGE_SIZE, '\0');
	}
	sBLE.message_count_fromPhone = 0; //all messages have been processed. Can begin adding to queue again
}

static void BLE_handleReceiveFlag_fromPods(void)
{
	//add messages in the UART buffer to the queue
	//process all messages in queue
	//clear queue

	osDelay(250); //wait for all possible response messages to finish coming in.
	osEventFlagsClear(BLE_Flags,BLE_MESSAGE_RECEIVED_FROM_PODS); //waiting for messages to come in caused flag to be set again. Clear to prevent prematurely entering later functions.
	while(1)
	{
		//TODO: Check if queue is full, if so return
		uint8_t length = UART_getPacket_fromPods(sBLE.rxQueue_fromPods.entry[sBLE.message_count_fromPods].data);
		if(length == 0)
			break; //no message added to queue
		else
		{
			sBLE.message_count_fromPods++;
		}
	}

	for(uint8_t queueEntry=0; queueEntry < sBLE.message_count_fromPods; queueEntry++)
	{
		if(BLE_searchForKeyword((uint8_t *)"RCV=", queueEntry, &sBLE.rxQueue_fromPods))
		{
			
		#ifdef IS_HUB_DEVICE //hub device is only one who should be receiving acks. If this is a hub device, check to see if we get an ack.
			if(BLE_searchForKeyword((uint8_t *)"ACK", queueEntry, &sBLE.rxQueue_fromPods)) //if we get an ack
			{
				sBLE.BLE_currentCommsState_forPods = ACK_RECEIVED;
				osEventFlagsSet(BLE_Flags,BLE_ACK_RECEIVED); //signal that ack has been received, and app thread can start running again
				break; //don't go on to try and give app task the received data. This data was for us.
			}
		#endif
			Util_copyMemory(&sBLE.rxQueue_fromPods.entry[queueEntry].data[4], sBLE.rxPacket.dataBuffer, strlen((const char *)sBLE.rxQueue_fromPods.entry[queueEntry].data)-6); //-6 for RCV=\r\n
			if(BLE_validatePacket())
			{
				osEventFlagsSet(APP_Request_Flags, APP_MESSAGE_PENDING_FROM_BLE);
				osMessageQueuePut(receivedMessageQ_id,&sBLE.rxPacket.pkt.data,NULL,1000); //send received message to app thread
				osEventFlagsWait(BLE_Flags,BLE_RECEIVED_MESSAGE_TRANSFERRED,osFlagsWaitAll,3000); //wait until received message has been copied over to app thread variable before deleting.
				osMessageQueueReset(receivedMessageQ_id); //TESTING. DOES THIS PREVENT REPEAT MESSAGES?
			}
		}
		else if(BLE_searchForKeyword((uint8_t *)"OK", queueEntry, &sBLE.rxQueue_fromPods))
		{
			//Change from "waiting on response" to "response received" state
			sBLE.BLE_currentModuleState_forPods = RESPONSE_RECEIVED;
		}
		else if(BLE_searchForKeyword((uint8_t *)"ERR", queueEntry, &sBLE.rxQueue_fromPods))
		{
			//Change from "waiting on response" to "error" state
			sBLE.BLE_currentModuleState_forPods = ERROR_STATE;
		}
		else if(BLE_searchForKeyword((uint8_t *)"READY", queueEntry, &sBLE.rxQueue_fromPods))
		{
			//Change from "waiting on response" to "response received" state
			sBLE.BLE_currentModuleState_forPods = RESPONSE_RECEIVED;
		}
		#ifdef IS_HUB_DEVICE
		else if(BLE_searchForKeyword((uint8_t *)"SCN=", queueEntry, &sBLE.rxQueue_fromPods)) //send scan results over to app thread so that it can find the phone's bluetooth address
		{
			if(sBLE.rxQueue_fromPods.entry[queueEntry].data[30] == '1') //1 in this position (after the M) means it is melodysmart enabled
			{
				Util_copyMemory(&sBLE.rxQueue_fromPods.entry[queueEntry].data[6],(uint8_t *)scannedAddress,12);
				if(strncmp(scannedAddress,podInfoList.Pod1_Address,12) == 0)
					podInfoList.Pod1_Online = true;
				else if(strncmp(scannedAddress,podInfoList.Pod2_Address,12) == 0)
					podInfoList.Pod2_Online = true;
				else if(strncmp(scannedAddress,podInfoList.Pod3_Address,12) == 0)
					podInfoList.Pod3_Online = true;
				osEventFlagsSet(BLE_Flags,BLE_MELODYSMART_ADDRESS_FOUND); //TODO: Remove?
			}
		}
		else if(BLE_searchForKeyword((uint8_t *)"STS=C CON", queueEntry, &sBLE.rxQueue_fromPods)) //we are successfully connected
		{
			//Change from "waiting to connection" to "connected" state
			sBLE.BLE_currentCommsState_forPods = CONNECTED_IDLE;
			sBLE.BLE_currentModuleState_forPods = RESPONSE_RECEIVED;
			Util_copyMemory(&sBLE.rxQueue_fromPods.entry[queueEntry].data[10],(uint8_t *)connectedDeviceAddress,12);
			osEventFlagsSet(BLE_Flags,BLE_CONNECTED_TO_POD); //signals that a requested connection to a device was successfully made
			
			
			//Change from "waiting on response" to "response received" state
			sBLE.BLE_currentModuleState_forPods = RESPONSE_RECEIVED;
		}
		#endif //IS_HUB_DEVICE
		
		//once message has been processed, or if it was not something we care about, clear out that entry
		Util_fillMemory(sBLE.rxQueue_fromPods.entry[queueEntry].data, MESSAGE_SIZE, '\0');
	}
	sBLE.message_count_fromPods = 0; //all messages have been processed. Can begin adding to queue again
}

#ifdef IS_HUB_DEVICE
static void BLE_SendCommand(const char *pString)
{
	//this function will use BLE_SendPacket, mixed with retries, to help ensure faithful communication to other bluetooth device.
	//Only the hub will be using this function (to send instructions to pod devices). The pod devices will send back ACKs using the BLE_SendAck function.
	uint8_t packetLength = BLE_BuildPacket(pString);
	uint32_t startingTickCount;
	uint32_t currentTickCount;
	if(packetLength > 0)
	{
		//send packet
		//when ack is received, state will change
		for(uint8_t retries=0; retries < COMMS_MAX_RETRIES; retries++)
		{
			sBLE.BLE_currentCommsState_forPods = SENDING_PACKET;
			BLE_SendPacket(packetLength, SEND_TO_POD);
			sBLE.BLE_currentCommsState_forPods = AWAITING_ACK;
			
			startingTickCount = osKernelGetTickCount();
			do
			{
				osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED_FROM_PODS,osFlagsWaitAll,100);
				BLE_handleReceiveFlag_fromPods();
				currentTickCount = osKernelGetTickCount();
			}while(((osEventFlagsGet(BLE_Flags) & BLE_ACK_RECEIVED)!=BLE_ACK_RECEIVED)&&(currentTickCount < (startingTickCount+3000)));
			
			//flags = osEventFlagsWait(BLE_Flags,BLE_ACK_RECEIVED,osFlagsWaitAll,3000); //suspend app thread execution until ack is received
			if(osEventFlagsGet(BLE_Flags) & BLE_ACK_RECEIVED)
			{
				osEventFlagsClear(BLE_Flags,BLE_ACK_RECEIVED);
				sBLE.BLE_currentCommsState_forPods = ACK_RECEIVED;
				break; //no need to retry
			}
		}
	}
}
#endif //IS_HUB_DEVICE

static void BLE_SendAck(void)
{
	uint8_t packetLength = BLE_BuildPacket("ACK");
	BLE_SendPacket(packetLength, 0); //in the case of BOTH the pod and the hub, the ack goes out UART0
	osEventFlagsClear(APP_Request_Flags,APP_SEND_ACK); //clear APP_SEND_ACK flag
}

static void BLE_waitOnDevice(uint8_t BLE_module)
{
	if(BLE_module == PHONE_BLE)
	{
		uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED_FROM_PHONE,osFlagsWaitAll,osWaitForever);
		BLE_handleReceiveFlag_fromPhone();
	}
	else
	{
		uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED_FROM_PODS,osFlagsWaitAll,osWaitForever);
		BLE_handleReceiveFlag_fromPods();
	}
	
}

static void BLE_initBLE_forPhone(void)
{
	int32_t result;
	do
	{
		result = BLE_stdCommand((uint8_t *)"rtr", PHONE_BLE);
	}while(result != SUCCESS);
	do
	{
		result = BLE_stdCommand((uint8_t *)"wrt", PHONE_BLE);
	}while(result != SUCCESS);
	BLE_issueResetCommand(PHONE_BLE);
}

#ifdef IS_HUB_DEVICE

static bool BLE_connectToDevice(const char *pBTAddress)
{
	bool result = false;
	
	if(sBLE.podConnected)
		BLE_disconnectFromDevice();
	
	sBLE.BLE_currentModuleState_forPods = SENDING_COMMAND;
	BLE_Send((uint8_t *)"CON ", 4, SEND_TO_POD);
	BLE_Send((uint8_t *)pBTAddress, BLE_ADDRESS_LENGTH, SEND_TO_POD);
	BLE_Send((uint8_t *)" 0\r", 3, SEND_TO_POD);
	sBLE.BLE_currentModuleState_forPods = AWAITING_RESPONSE;
	osDelay(500); //TESTING: wait to give connection time to establish
	result = BLE_isDeviceConnected();
	return result; //true if we successfully connected, else false
}

static bool BLE_isDeviceConnected(void) //used for pods only
{
	sBLE.BLE_currentModuleState_forPods = SENDING_COMMAND;
	BLE_Send((uint8_t *)"STS\r", 4, SEND_TO_POD);
	sBLE.BLE_currentModuleState_forPods = AWAITING_RESPONSE;
	osDelay(500); //Give time for all messages to come in so we can process them all at once
	uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED_FROM_PODS,osFlagsWaitAll,osWaitForever);
	BLE_handleReceiveFlag_fromPods();
	
	events = osEventFlagsGet(BLE_Flags);
	if((events & BLE_CONNECTED_TO_POD)&&(events != 0xFFFFFFFE))
	{
		osEventFlagsClear(BLE_Flags, BLE_CONNECTED_TO_POD); //need to clear flag manually since we did not "wait" on it
		//sBLE.BLE_currentCommState_forPods = CONNECTED_IDLE;
		sBLE.podConnected = true;
		return true;
	}
	else
	{
		return false;
	}
}

static void BLE_waitForPods(void)
{
	while((podInfoList.Pod1_Online == false)||(podInfoList.Pod2_Online == false)||(podInfoList.Pod3_Online == false))
	//while(podInfoList.Pod2_Online == false) //TODO: REMOVE AND REPLACE WITH ABOVE CODE. THIS IS FOR TESTING ONLY.
	//while((podInfoList.Pod1_Online == false)||(podInfoList.Pod2_Online == false)) //TODO: REMOVE AND REPLACE WITH ABOVE CODE (w/ all 3 pods). THIS IS FOR TESTING ONLY.
	{
		uint32_t events = osEventFlagsWait(BLE_Flags,BLE_MESSAGE_RECEIVED_FROM_PODS,osFlagsWaitAll,osWaitForever);
		BLE_handleReceiveFlag_fromPods();
	}
	osEventFlagsClear(APP_Request_Flags, APP_WAIT_FOR_PODS); //clear APP_WAIT_FOR_PODS flag
}


static void BLE_changePodStates(void)
{
	uint8_t requestedPodStates[3];
	uint8_t retriesRemaining;
	uint32_t testingResult = osMessageQueueGet(requestedPodStatesQ_id,&requestedPodStates,NULL,1000);
	bool connectionResults[3];
	bool connectionResult;

	if((podInfoList.Pod1_Current_State != requestedPodStates[0]) && (requestedPodStates[0]!=REMAIN_SAME))
	{
		retriesRemaining = 5;
		do
		{
			connectionResult = BLE_connectToDevice(podInfoList.Pod1_Address);
			retriesRemaining--;
		}while((connectionResult == false)&&(retriesRemaining > 0));
		if(connectionResult == true) //if we were able to successfully connect in certain amount of retries, then send command
		{
			podInfoList.Pod1_Online = true;
			if(requestedPodStates[0]==SAFE)
				BLE_SendCommand("SAFE");
			else if(requestedPodStates[0]==UNSAFE)
				BLE_SendCommand("UNSAFE");
			else if(requestedPodStates[0]==WARNING)
				BLE_SendCommand("WARNING");
			else if(requestedPodStates[0]==OFF)
				BLE_SendCommand("OFF");
			else if(requestedPodStates[0]==RUN)
				BLE_SendCommand("RUN");
			else if(requestedPodStates[0]==WALK)
				BLE_SendCommand("WALK");
			else if(requestedPodStates[0]==STOP)
				BLE_SendCommand("STOP");
			podInfoList.Pod1_Current_State = requestedPodStates[0];
		}
		else //indicate that pod is offline
		{
			podInfoList.Pod1_Online = false;
		}
	}
	
	if((podInfoList.Pod2_Current_State != requestedPodStates[1]) && (requestedPodStates[1]!=REMAIN_SAME))
	{
		retriesRemaining = 5;
		do
		{
			connectionResult = BLE_connectToDevice(podInfoList.Pod2_Address);
			retriesRemaining--;
		}while((connectionResult == false)&&(retriesRemaining > 0));
		if(connectionResult == true) //if we were able to successfully connect in certain amount of retries, then send command
		{
			podInfoList.Pod2_Online = true;
			if(requestedPodStates[1]==SAFE)
				BLE_SendCommand("SAFE");
			else if(requestedPodStates[1]==UNSAFE)
				BLE_SendCommand("UNSAFE");
			else if(requestedPodStates[1]==WARNING)
				BLE_SendCommand("WARNING");
			else if(requestedPodStates[1]==OFF)
				BLE_SendCommand("OFF");
			else if(requestedPodStates[1]==RUN)
				BLE_SendCommand("RUN");
			else if(requestedPodStates[1]==WALK)
				BLE_SendCommand("WALK");
			else if(requestedPodStates[1]==STOP)
				BLE_SendCommand("STOP");
			podInfoList.Pod2_Current_State = requestedPodStates[1];
		}
		else //indicate that pod is offline
		{
			podInfoList.Pod2_Online = false;
		}
	}
	
	if((podInfoList.Pod3_Current_State != requestedPodStates[2]) && (requestedPodStates[2]!=REMAIN_SAME))
	{
		retriesRemaining = 5;
		do
		{
			connectionResult = BLE_connectToDevice(podInfoList.Pod3_Address);
			retriesRemaining--;
		}while((connectionResult == false)&&(retriesRemaining > 0));
		if(connectionResult == true) //if we were able to successfully connect in certain amount of retries, then send command
		{
			podInfoList.Pod3_Online = true;
			if(requestedPodStates[2]==SAFE)
				BLE_SendCommand("SAFE");
			else if(requestedPodStates[2]==UNSAFE)
				BLE_SendCommand("UNSAFE");
			else if(requestedPodStates[2]==WARNING)
				BLE_SendCommand("WARNING");
			else if(requestedPodStates[2]==OFF)
				BLE_SendCommand("OFF");
			else if(requestedPodStates[2]==RUN)
				BLE_SendCommand("RUN");
			else if(requestedPodStates[2]==WALK)
				BLE_SendCommand("WALK");
			else if(requestedPodStates[2]==STOP)
				BLE_SendCommand("STOP");
			podInfoList.Pod3_Current_State = requestedPodStates[2];
		}
		else //indicate that pod is offline
		{
			podInfoList.Pod3_Online = false;
		}
	}

	osEventFlagsClear(APP_Request_Flags,APP_CHANGE_POD_STATES); //clear APP_CONNECT_TO_POD flag
	//send APP thread the results from pod state change request (which pods were able to be changed or not)
	connectionResults[0] = podInfoList.Pod1_Online;
	connectionResults[1] = podInfoList.Pod2_Online;
	connectionResults[2] = podInfoList.Pod3_Online;
	osMessageQueuePut(podStateRequestResultsQ_id,&connectionResults,NULL,osWaitForever);
}

static void BLE_disconnectFromDevice(void) //will only be disconnecting from pods
{
	BLE_stdCommand((uint8_t *)"DCN", SEND_TO_POD);
	sBLE.podConnected = false;
}

static void BLE_initBLE_forPods(void)
{
	int32_t result;
	do
	{
		result = BLE_stdCommand((uint8_t *)"set cent=on", PODS_BLE);
	}while(result != SUCCESS);
	do
	{
		result = BLE_stdCommand((uint8_t *)"set acon=off", PODS_BLE);
	}while(result != SUCCESS);
	do
	{
		result = BLE_stdCommand((uint8_t *)"set scnp=000F4240 00002BF2", PODS_BLE); //set scan interval to 1000000us (0x000F4240) to prevent UART from being overwhelmed
		//result = BLE_stdCommand((uint8_t *)"set scnp=0003D090 00002BF2", UART_num); //set scan interval to 250000us (0x0003D090) to prevent UART from being overwhelmed
	}while(result != SUCCESS);
	do
	{
		result = BLE_stdCommand((uint8_t *)"wrt", PODS_BLE);
	}while(result != SUCCESS);
	BLE_issueResetCommand(PODS_BLE);
}



#endif //IS_HUB_DEVICE


