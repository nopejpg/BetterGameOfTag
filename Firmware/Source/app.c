#include "RTE_Components.h"
#include CMSIS_device_header
#include "cmsis_os2.h"
#include "app.h"
#include "BLE.h"
#include "DMA.h"
#include <stdlib.h>
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


static bool currentPodStatuses[3];

#endif //IS_HUB_DEVICE


osThreadId_t tid_APP;
osEventFlagsId_t DMA_flags;
osEventFlagsId_t APP_Request_Flags;
osTimerId_t APP_AutoTagChangeTimer_id;
osTimerId_t APP_AutoTagWarningTimer_id;
osMessageQueueId_t receivedMessageQ_id;
osMessageQueueId_t deviceConnectionRequestQ_id;
osMessageQueueId_t requestedPodStatesQ_id;
osMessageQueueId_t podStateRequestResultsQ_id;
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
static void App_waitForPods(void); //attempts to connect to all pods. Returns when connected.
//static void App_changePodStates(uint8_t pod1StateCommand, uint8_t pod2StateCommand, uint8_t pod3StateCommand);
static void App_changePodStates(uint8_t * podStateCommands);
static void App_playManualTag(void);
static void App_playAutomaticTag(void);
static void App_playRLGL(void);

static void App_autoTagChangeTimer_Callback(void *arg);
static void App_autoTagWarningTimer_Callback(void *arg);

void Thread_APP_HUB(void *arg)
{
	APP_Request_Flags = osEventFlagsNew(NULL);
	APP_AutoTagChangeTimer_id = osTimerNew(App_autoTagChangeTimer_Callback,osTimerOnce,NULL,NULL);
	APP_AutoTagWarningTimer_id = osTimerNew(App_autoTagWarningTimer_Callback,osTimerOnce,NULL,NULL);
	uint32_t flags = osThreadFlagsWait(BLE_INIT_AND_CONNECTED,osFlagsWaitAll,osWaitForever); //ensure BLE module is set up before attempting to send commands to it
	
	Control_RGB_LEDs(1,0,0); //debug LEDs
	
	HubState = PODS_AWAITING_CONNECTION;

	App_waitForPods(); //on power up, make sure all pods are online before proceeding
	
	HubState = HUB_READY;
	
	while(1)
	{
		Control_RGB_LEDs(0,1,0); //debug LEDs
		uint32_t result = osEventFlagsWait(APP_Request_Flags, APP_MESSAGE_PENDING_FROM_BLE|APP_RESET_TO_MAIN_MENU, osFlagsWaitAny, osWaitForever);
		if(result & APP_RESET_TO_MAIN_MENU) //if BLE thread tells us that phone disconnected from us...
		{
			continue;
		}
		result = osMessageQueueGet(receivedMessageQ_id, &sAPP.rxMessage, NULL, 1000); //wait until command from Phone App is received
		if(result == osOK)
		{
			osEventFlagsSet(BLE_Flags,BLE_RECEIVED_MESSAGE_TRANSFERRED); //let BLE module know that we are done with it's data, and that it is free to clear it
			if(strstr((const char *)sAPP.rxMessage.dataBuffer,"MAN_TAG") != NULL) //if we are commanded to play manual tag
			{
				App_SendAck();
				HubState = MANUAL_TAG;
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"AUTOMATE_TAG") != NULL) //if we are commanded to play automatic tag
			{
				App_SendAck();
				HubState = AUTO_TAG;
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"RL_GL") != NULL) //if we are commanded to play red-light/green-light
			{
				App_SendAck();
				HubState = RL_GL;
			}
		}
		if(HubState == MANUAL_TAG)
		{
			App_playManualTag();
		}
		else if(HubState == AUTO_TAG)
		{
			App_playAutomaticTag();
		}
		else if(HubState == RL_GL)
		{
			App_playRLGL();
		}
	}
}

static void App_waitForPods(void)
{
	osEventFlagsSet(APP_Request_Flags,APP_WAIT_FOR_PODS); //set what request we are making of the BLE
	osEventFlagsSet(BLE_Flags,APP_THREAD_REQESTING_ACTION); //Let BLE thread know that we have a request for it
	osEventFlagsWait(APP_Request_Flags,APP_REQUEST_COMPLETE,NULL,osWaitForever); //wait until BLE is done processing request
}

static void App_changePodStates(uint8_t * podStateCommands)
{
	osMessageQueuePut(requestedPodStatesQ_id,podStateCommands,NULL,osWaitForever); //set which pod to connect to
	osEventFlagsSet(APP_Request_Flags,APP_CHANGE_POD_STATES); //set what request we are making of the BLE
	osEventFlagsSet(BLE_Flags,APP_THREAD_REQESTING_ACTION); //Let BLE thread know that we have a request for it
	osEventFlagsWait(APP_Request_Flags,APP_REQUEST_COMPLETE,NULL,osWaitForever); //wait until BLE is done processing request
	//osMessageQueueGet(podStateRequestResultsQ_id, &currentPodStatuses,NULL,osWaitForever); //retrieve the most recent connection statuses of the pods
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
		
		uint32_t result = osEventFlagsWait(APP_Request_Flags, APP_MESSAGE_PENDING_FROM_BLE | APP_RESET_TO_MAIN_MENU, NULL, 1000);
		if((result & APP_RESET_TO_MAIN_MENU)&&(result != osFlagsErrorTimeout)) //if BLE thread tells us that phone disconnected from us...
		{
			HubState = HUB_READY; //return to main menu
			continue;
		}
		result = osMessageQueueGet(receivedMessageQ_id, &sAPP.rxMessage, NULL, 1000); //wait until command from Phone App is received
		if(result == osOK)
		{
			osEventFlagsSet(BLE_Flags,BLE_RECEIVED_MESSAGE_TRANSFERRED); //let BLE module know that we are done with it's data, and that it is free to clear it
			if(sAPP.rxMessage.dataBuffer[0] == '%') //check if this is a command
			{
				App_SendAck();
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
				App_SendAck();
				HubState = HUB_READY;
			}
		}
	}
}

static void App_playAutomaticTag(void)
{
	/*
	Algorithm for automatic tag:
	1. Initialize bases to some random states
	2. Start timer for next time bases change
	3. Start timer for next time warning states should be set (this will always be a shorter timer than the "change" timer
	4. Wait for warning timer to expire
	5. When the warning timer expires, calculate the next state values
	6. Based on which states will be safe/unsafe, set the warning states accordingly.
	7. Wait for change timer to expire
	8. When the change timer expires, set pods to previously calculated states
	9. Reset warning and change timers
	10. Repeat steps 4-9 until EXIT_GAME command is given
	*/
	
	//initialize pods with random states and kick off timers
	static uint8_t podStateRequest[3];
	static uint8_t previousPodStateRequest[3];
	static uint8_t warningPodStateRequest[3];
	podStateRequest[0] = (rand() % 2) + 1; //should give value between [1,2]
	podStateRequest[1] = (rand() % 2) + 1;
	podStateRequest[2] = (rand() % 2) + 1;
	previousPodStateRequest[0] = podStateRequest[0];
	previousPodStateRequest[1] = podStateRequest[1];
	previousPodStateRequest[2] = podStateRequest[2];
	App_changePodStates(podStateRequest); //change pod states
	osTimerStart(APP_AutoTagChangeTimer_id, 20000);
	osTimerStart(APP_AutoTagWarningTimer_id, 20000 - 5000); //set warning states 5 seconds before unsafe states
	
	while(HubState == AUTO_TAG)
	{
		uint32_t result = osEventFlagsWait(APP_Request_Flags, APP_AUTO_TAG_CHANGE_TIMER_EXPIRED|APP_AUTO_TAG_WARNING_TIMER_EXPIRED|APP_MESSAGE_PENDING_FROM_BLE|APP_RESET_TO_MAIN_MENU, NULL, osWaitForever);
		if(result & APP_MESSAGE_PENDING_FROM_BLE)
		{
			uint32_t result = osMessageQueueGet(receivedMessageQ_id, &sAPP.rxMessage, NULL, 1000);
			if(result == osOK)
			{
				osEventFlagsSet(BLE_Flags,BLE_RECEIVED_MESSAGE_TRANSFERRED); //let BLE module know that we are done with it's data, and that it is free to clear it
				if(strstr((const char *)sAPP.rxMessage.dataBuffer,"EXIT_GAME") != NULL)
				{
					App_SendAck();
					HubState = HUB_READY;
				}
			}
		}
		else if(result & APP_AUTO_TAG_CHANGE_TIMER_EXPIRED) //when time for new states, randomize and change to new states
		{
			App_changePodStates(podStateRequest);
			osTimerStart(APP_AutoTagChangeTimer_id, 20000); //start timer here (for ~20 seconds?)
			osTimerStart(APP_AutoTagWarningTimer_id, 20000 - 5000); //set warning states 5 seconds before unsafe states
			
		}
		else if(result & APP_AUTO_TAG_WARNING_TIMER_EXPIRED)
		{
			//Set bases that are to be unsafe to warning states first
			podStateRequest[0] = (rand() % 2) + 1;
			podStateRequest[1] = (rand() % 2) + 1;
			podStateRequest[2] = (rand() % 2) + 1;
			warningPodStateRequest[0] = ((podStateRequest[0] == UNSAFE)&&(previousPodStateRequest[0] != UNSAFE)) ? WARNING:REMAIN_SAME; //if next value is unsafe, then send warning first
			warningPodStateRequest[1] = ((podStateRequest[1] == UNSAFE)&&(previousPodStateRequest[1] != UNSAFE)) ? WARNING:REMAIN_SAME;
			warningPodStateRequest[2] = ((podStateRequest[2] == UNSAFE)&&(previousPodStateRequest[2] != UNSAFE)) ? WARNING:REMAIN_SAME;
			previousPodStateRequest[0] = podStateRequest[0];
			previousPodStateRequest[1] = podStateRequest[1];
			previousPodStateRequest[2] = podStateRequest[2];
			App_changePodStates(warningPodStateRequest); //set relevant pods to warning state
		}
		else if(result & APP_RESET_TO_MAIN_MENU) //if BLE thread tells us that phone disconnected from us...
		{
			HubState = HUB_READY; //return to main menu
		}
	}
}

static void App_playRLGL(void)
{
	static uint8_t podStateRequest[3];
	while(HubState == RL_GL)
	{
		uint32_t result = osEventFlagsWait(APP_Request_Flags, APP_MESSAGE_PENDING_FROM_BLE | APP_RESET_TO_MAIN_MENU, NULL, osWaitForever);
		if(result & APP_MESSAGE_PENDING_FROM_BLE)
		{
			uint32_t result = osMessageQueueGet(receivedMessageQ_id, &sAPP.rxMessage, NULL, 1000);
			if(result == osOK)
			{
				osEventFlagsSet(BLE_Flags,BLE_RECEIVED_MESSAGE_TRANSFERRED); //let BLE module know that we are done with it's data, and that it is free to clear it
				if(strstr((const char *)sAPP.rxMessage.dataBuffer,"RUN") != NULL)
				{
					App_SendAck();
					Util_copyMemory((uint8_t[]){OFF, OFF, OFF}, podStateRequest, 3); //turn off all pods
					App_changePodStates(podStateRequest);
					podStateRequest[0] = RUN; 
					App_changePodStates(podStateRequest); //set desired state
				}
				else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"WALK") != NULL)
				{
					App_SendAck();
					Util_copyMemory((uint8_t[]){OFF, OFF, OFF}, podStateRequest, 3);
					App_changePodStates(podStateRequest);
					podStateRequest[1] = WALK;
					App_changePodStates(podStateRequest);
				}
				else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"STOP") != NULL)
				{
					App_SendAck();
					Util_copyMemory((uint8_t[]){OFF, OFF, OFF}, podStateRequest, 3);
					App_changePodStates(podStateRequest);
					podStateRequest[2] = STOP;
					App_changePodStates(podStateRequest);
				}
				else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"EXIT_GAME") != NULL)
				{
					App_SendAck();
					HubState = HUB_READY;
				}
			}
		}
		else if(result & APP_RESET_TO_MAIN_MENU) //if BLE thread tells us that phone disconnected from us...
		{
			HubState = HUB_READY; //return to main menu
		}
	}
}



static void App_autoTagChangeTimer_Callback(void *arg)
{
	//function used for telling app thread that it is time to change bases again
	osEventFlagsSet(APP_Request_Flags,APP_AUTO_TAG_CHANGE_TIMER_EXPIRED);
}

static void App_autoTagWarningTimer_Callback(void *arg)
{
	//function used for telling app thread that it is time to set warnings again
	osEventFlagsSet(APP_Request_Flags,APP_AUTO_TAG_WARNING_TIMER_EXPIRED);
}

#else
static void playBadNoise(void);
static void playGoodNoise(void);

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
				Control_My_LEDs(1,0,0);
				Play_Recording(Not_Audio,sizeof(Not_Audio)/sizeof(Not_Audio[0]));
				osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
				Play_Recording(Safe_Audio,sizeof(Safe_Audio)/sizeof(Safe_Audio[0]));
				osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"SAFE") != NULL) //if UNSAFE message
			{
				App_SendAck();
				Control_RGB_LEDs(0,1,0);
				Control_My_LEDs(0,1,0);
				Play_Recording(Safe_Audio,sizeof(Safe_Audio)/sizeof(Safe_Audio[0]));
				osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"WARNING") != NULL) //if WARNING message
			{
				App_SendAck();
				Control_RGB_LEDs(1,1,0);
				Control_My_LEDs(1,1,0);
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"OFF") != NULL) //if WARNING message
			{
				App_SendAck();
				Control_RGB_LEDs(0,0,0);
				Control_My_LEDs(0,0,0);
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"RUN") != NULL) //if GO message
			{
				App_SendAck();
				Control_RGB_LEDs(0,1,0);
				Control_My_LEDs(0,1,0);
				playGoodNoise();
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"WALK") != NULL) //if GO message
			{
				App_SendAck();
				Control_RGB_LEDs(1,1,0);
				Control_My_LEDs(1,1,0);
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"STOP") != NULL) //if STOP message
			{
				App_SendAck();
				Control_RGB_LEDs(1,0,0);
				Control_My_LEDs(1,0,0);
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

