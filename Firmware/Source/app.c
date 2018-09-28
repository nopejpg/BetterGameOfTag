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


osThreadId_t tid_APP;
osEventFlagsId_t DMA_flags;
osMessageQueueId_t receivedMessageQ_id;
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

#ifdef IS_HUB_DEVICE
void Thread_APP_HUB(void *arg)
{
	uint32_t flags = osThreadFlagsWait(BLE_INIT_AND_CONNECTED,osFlagsWaitAll,osWaitForever); //ensure BLE module is set up before attempting to send commands to it
	while(1)
	{
		BLE_SendCommand("SAFE");
		osDelay(3000);
		BLE_SendCommand("UNSAFE");
		osDelay(3000);
	}
}
#else
void playBadNoise(void);
void playGoodNoise(void);

void Thread_APP_POD(void *arg)
{
	while(1)
	{
		result = osMessageQueueGet(receivedMessageQ_id, &sAPP.rxMessage, NULL, 1000);
		if(result == osOK)
		{
			osEventFlagsSet(BLE_Flags,BLE_RECEIVED_MESSAGE_TRANSFERRED); //let BLE module know that we are done with it's data, and that it is free to clear it
			if(strstr((const char *)sAPP.rxMessage.dataBuffer,"UNSAFE") != NULL) //if SAFE message
			{
				BLE_SendAck();
				Control_RGB_LEDs(1,0,0);
				Play_Recording(Not_Audio,sizeof(Not_Audio)/sizeof(Not_Audio[0]));
				osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
				Play_Recording(Safe_Audio,sizeof(Safe_Audio)/sizeof(Safe_Audio[0]));
				osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"SAFE") != NULL) //if UNSAFE message
			{
				BLE_SendAck();
				Control_RGB_LEDs(0,1,0);
				Play_Recording(Safe_Audio,sizeof(Safe_Audio)/sizeof(Safe_Audio[0]));
				osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"WARNING") != NULL) //if WARNING message
			{
				BLE_SendAck();
				Control_RGB_LEDs(1,1,0);
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"GO") != NULL) //if GO message
			{
				BLE_SendAck();
				Control_RGB_LEDs(0,1,0);
//				Play_Recording(Go_Audio,sizeof(Go_Audio)/sizeof(Go_Audio[0]));
//				osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
				playGoodNoise();
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"STOP") != NULL) //if STOP message
			{
				BLE_SendAck();
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




