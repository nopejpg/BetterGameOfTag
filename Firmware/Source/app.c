#include "RTE_Components.h"
#include CMSIS_device_header
#include "cmsis_os2.h"
#include "app.h"
#include "BLE.h"
#include "DMA.h"
//#include "safe_female.h"
//#include "not_female.h"
#ifndef IS_HUB_DEVICE
	#include "Safe_Audio.h"
	#include "Not_Audio.h"
	//#include "Go_Audio.h"
	//#include "Stop_Audio.h"
	#include "BadNoise.h"
	#include "GoodNoise.h"
#endif //IS_HUB_DEVICE

#include "LEDs.h"
#include "string.h"
#include "utilities.h"

#ifdef IS_HUB_DEVICE

static enum
{
	SEARCHING_FOR_PHONE_ADDRESS,
	PODS_AWAITING_CONNECTION,
	PODS_CONNECTED,
	HUB_READY,
	MANUAL_TAG,
	AUTO_TAG,
	RL_GL
}HubState;


#endif //IS_HUB_DEVICE


osThreadId_t tid_APP;
osEventFlagsId_t DMA_flags;
osEventFlagsId_t APP_Request_Flags;
osMessageQueueId_t receivedMessageQ_id;
osMessageQueueId_t deviceConnectionRequestQ_id;
osMessageQueueId_t requestedPodStatesQ_id;
osStatus_t result;


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

static struct
{
	Packet rxMessage;
}sAPP;

static void App_SendAck(void);
#ifdef IS_HUB_DEVICE
static void App_findPhoneAddress(void); //tell BLE to search incoming scans until address other than pod addresses are found
static void App_connectToPhone(void);
static void App_waitForPods(void); //attempts to connect to all pods. Returns when connected.
//static void App_changePodStates(uint8_t pod1StateCommand, uint8_t pod2StateCommand, uint8_t pod3StateCommand);
static void App_changePodStates(uint8_t * podStateCommands);
static void App_playManualTag(void);
static void App_playAutomaticTag(void);
static void App_playRLGL(void);
static void App_connectToPod(uint32_t Pod_ID);

static void App_enterCentralMode(void);
static void App_enterPeripheralMode(void);

void Thread_APP_HUB(void *arg)
{
	APP_Request_Flags = osEventFlagsNew(NULL);
	uint32_t flags = osThreadFlagsWait(BLE_INIT_AND_CONNECTED,osFlagsWaitAll,osWaitForever); //ensure BLE module is set up before attempting to send commands to it
	
	#ifndef PERIPHERAL_WORKAROUND
	HubState = SEARCHING_FOR_PHONE_ADDRESS;
	App_findPhoneAddress();
	Control_RGB_LEDs(1,0,0); //debug LEDs
	
	HubState = PODS_AWAITING_CONNECTION;
	App_waitForPods(); //on power up, make sure all pods are online before proceeding
	
	App_connectToPhone();
	#endif
	
	HubState = HUB_READY;
	
	while(1)
	{
		Control_RGB_LEDs(0,1,0); //debug LEDs
		result = osMessageQueueGet(receivedMessageQ_id, &sAPP.rxMessage, NULL, 1000); //wait until command from Phone App is received
		if(result == osOK)
		{
			osEventFlagsSet(BLE_Flags,BLE_RECEIVED_MESSAGE_TRANSFERRED); //let BLE module know that we are done with it's data, and that it is free to clear it
			if(strstr((const char *)sAPP.rxMessage.dataBuffer,"MAN_TAG") != NULL) //if we are commanded to play manual tag
			{
				#ifndef PERIPHERAL_WORKAROUND
				//App_SendAck();
				#endif
				
				HubState = MANUAL_TAG;
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"AUTO_TAG") != NULL) //if we are commanded to play automatic tag
			{
				//TODO
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"RL_GL") != NULL) //if we are commanded to play red-light/green-light
			{
				//TODO
			}
		}
		if(HubState == MANUAL_TAG)
		{
			App_playManualTag();
		}
		else if(HubState == AUTO_TAG)
		{
			
		}
		else if(HubState == RL_GL)
		{
			
		}
	}
}

static void App_connectToPod(uint32_t Pod_ID)
{
	bool connectionResult = false;
	while(connectionResult == false)
	{
		osMessageQueuePut(deviceConnectionRequestQ_id,&Pod_ID,NULL,osWaitForever); //set which pod to connect to
		osEventFlagsSet(APP_Request_Flags,APP_CONNECT_TO_POD); //set what request we are making of the BLE
		osEventFlagsSet(BLE_Flags,APP_THREAD_REQESTING_ACTION); //Let BLE thread know that we have a request for it
		osEventFlagsWait(APP_Request_Flags,APP_REQUEST_COMPLETE,NULL,osWaitForever); //wait until BLE is done processing request
		
		osMessageQueueGet(deviceConnectionRequestQ_id,&connectionResult,NULL,osWaitForever); //check response from message queue here
	}
}

static void App_findPhoneAddress(void)
{
	osEventFlagsSet(APP_Request_Flags,APP_FIND_PHONE_ADDRESS); //set what request we are making of the BLE
	osEventFlagsSet(BLE_Flags,APP_THREAD_REQESTING_ACTION); //Let BLE thread know that we have a request for it
	osEventFlagsWait(APP_Request_Flags,APP_REQUEST_COMPLETE,NULL,osWaitForever); //wait until BLE is done processing request
}

static void App_waitForPods(void)
{
	osEventFlagsSet(APP_Request_Flags,APP_WAIT_FOR_PODS); //set what request we are making of the BLE
	osEventFlagsSet(BLE_Flags,APP_THREAD_REQESTING_ACTION); //Let BLE thread know that we have a request for it
	osEventFlagsWait(APP_Request_Flags,APP_REQUEST_COMPLETE,NULL,osWaitForever); //wait until BLE is done processing request
}

static void App_connectToPhone(void)
{
	bool connectionResult = false;
	while(connectionResult == false)
	{
		osEventFlagsSet(APP_Request_Flags,APP_CONNECT_TO_PHONE); //set what request we are making of the BLE
		osEventFlagsSet(BLE_Flags,APP_THREAD_REQESTING_ACTION); //Let BLE thread know that we have a request for it
		osEventFlagsWait(APP_Request_Flags,APP_REQUEST_COMPLETE,NULL,osWaitForever); //wait until BLE is done processing request
		osMessageQueueGet(deviceConnectionRequestQ_id,&connectionResult,NULL,osWaitForever); //check response from message queue here
	}
}

static void App_changePodStates(uint8_t * podStateCommands)
{
	//osMessageQueuePut(requestedPodStatesQ_id,&podStateCommands,NULL,osWaitForever); //set which pod to connect to
	osMessageQueuePut(requestedPodStatesQ_id,podStateCommands,NULL,osWaitForever); //set which pod to connect to
	osEventFlagsSet(APP_Request_Flags,APP_CHANGE_POD_STATES); //set what request we are making of the BLE
	osEventFlagsSet(BLE_Flags,APP_THREAD_REQESTING_ACTION); //Let BLE thread know that we have a request for it
	osEventFlagsWait(APP_Request_Flags,APP_REQUEST_COMPLETE,NULL,osWaitForever); //wait until BLE is done processing request
}

static void App_playManualTag(void)
{
	while(HubState == MANUAL_TAG)
	{
		static uint8_t podStateRequest[3];
		uint8_t pod1StateCommand;
		uint8_t pod2StateCommand;
		uint8_t pod3StateCommand;
		Control_RGB_LEDs(0,0,1); //debug LEDs
		
		result = osMessageQueueGet(receivedMessageQ_id, &sAPP.rxMessage, NULL, 1000); //wait until command from Phone App is received
		if(result == osOK)
		{
			osEventFlagsSet(BLE_Flags,BLE_RECEIVED_MESSAGE_TRANSFERRED); //let BLE module know that we are done with it's data, and that it is free to clear it
			if(sAPP.rxMessage.dataBuffer[0] == '%') //check if this is a command
			{
				#ifndef PERIPHERAL_WORKAROUND
				App_SendAck();
				#endif
				/*
				An example command to change states looks like this: %SUS
				3 positions after the %: one for each pod.
				S = SAFE
				U = UNSAFE
				*/
				
				if(sAPP.rxMessage.dataBuffer[1] == 'S')
					pod1StateCommand = SAFE;
				else if(sAPP.rxMessage.dataBuffer[1] == 'U')
					pod1StateCommand = UNSAFE;
				if(sAPP.rxMessage.dataBuffer[2] == 'S')
					pod2StateCommand = SAFE;
				else if(sAPP.rxMessage.dataBuffer[2] == 'U')
					pod2StateCommand = UNSAFE;
				if(sAPP.rxMessage.dataBuffer[3] == 'S')
					pod3StateCommand = SAFE;
				else if(sAPP.rxMessage.dataBuffer[3] == 'U')
					pod3StateCommand = UNSAFE;
				
				Util_copyMemory((uint8_t[]){pod1StateCommand, pod2StateCommand, pod3StateCommand}, podStateRequest, 3); //fill request array with desired states
				App_changePodStates(podStateRequest);
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"EXIT_GAME") != NULL)
			{
				#ifndef PERIPHERAL_WORKAROUND
				App_SendAck();
				#endif
				HubState = HUB_READY;
			}
		}
	}
}

void App_enterCentralMode(void)
{
	osEventFlagsSet(APP_Request_Flags,APP_ENTER_CENTRAL_MODE); //set what request we are making of the BLE
	osEventFlagsSet(BLE_Flags,APP_THREAD_REQESTING_ACTION); //Let BLE thread know that we have a request for it
	osEventFlagsWait(APP_Request_Flags,APP_REQUEST_COMPLETE,NULL,osWaitForever); //wait until BLE is done processing request
}

void App_enterPeripheralMode(void)
{
	osEventFlagsSet(APP_Request_Flags,APP_ENTER_PERIPHERAL_MODE); //set what request we are making of the BLE
	osEventFlagsSet(BLE_Flags,APP_THREAD_REQESTING_ACTION); //Let BLE thread know that we have a request for it
	osEventFlagsWait(APP_Request_Flags,APP_REQUEST_COMPLETE,NULL,osWaitForever); //wait until BLE is done processing request
}

#else
void playBadNoise(void);
void playGoodNoise(void);

void Thread_APP_POD(void *arg)
{
	APP_Request_Flags = osEventFlagsNew(NULL);
	while(1)
	{
		result = osMessageQueueGet(receivedMessageQ_id, &sAPP.rxMessage, NULL, 1000);
		if(result == osOK)
		{
			osEventFlagsSet(BLE_Flags,BLE_RECEIVED_MESSAGE_TRANSFERRED); //let BLE module know that we are done with it's data, and that it is free to clear it
			if(strstr((const char *)sAPP.rxMessage.dataBuffer,"UNSAFE") != NULL) //if SAFE message
			{
				App_SendAck();
				Control_RGB_LEDs(1,0,0);
				Play_Recording(Not_Audio,sizeof(Not_Audio)/sizeof(Not_Audio[0]));
				osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
				Play_Recording(Safe_Audio,sizeof(Safe_Audio)/sizeof(Safe_Audio[0]));
				osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"SAFE") != NULL) //if UNSAFE message
			{
				App_SendAck();
				Control_RGB_LEDs(0,1,0);
				Play_Recording(Safe_Audio,sizeof(Safe_Audio)/sizeof(Safe_Audio[0]));
				osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"WARNING") != NULL) //if WARNING message
			{
				App_SendAck();
				Control_RGB_LEDs(1,1,0);
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"GO") != NULL) //if GO message
			{
				App_SendAck();
				Control_RGB_LEDs(0,1,0);
//				Play_Recording(Go_Audio,sizeof(Go_Audio)/sizeof(Go_Audio[0]));
//				osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
				playGoodNoise();
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"STOP") != NULL) //if STOP message
			{
				App_SendAck();
				Control_RGB_LEDs(1,0,0);
//				Play_Recording(Stop_Audio,sizeof(Stop_Audio)/sizeof(Stop_Audio[0]));
//				osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
				playBadNoise();
			}
		}
	}
}

void playBadNoise(void)
{
	for(uint16_t i=0;i<143;i++) //each segment is ~0.014s long, so need to play ~143 times to get 2s work of sound
	{
		Play_Recording(BadNoise,sizeof(BadNoise)/sizeof(BadNoise[0]));
		osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
	}
}

void playGoodNoise(void)
{
	for(uint16_t i=0;i<100;i++) //each segment is ~0.02s long, so need to play ~100 times to get 2s work of sound
	{
		Play_Recording(GoodNoise,sizeof(GoodNoise)/sizeof(GoodNoise[0]));
		osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
	}
}
#endif

static void App_SendAck(void)
{
	osEventFlagsSet(APP_Request_Flags,APP_SEND_ACK); //set what request we are making of the BLE
	osEventFlagsSet(BLE_Flags,APP_THREAD_REQESTING_ACTION); //Let BLE thread know that we have a request for it
	osEventFlagsWait(APP_Request_Flags,APP_REQUEST_COMPLETE,NULL,osWaitForever); //wait until BLE is done processing request
}


