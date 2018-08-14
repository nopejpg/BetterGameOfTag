#include "RTE_Components.h"
#include CMSIS_device_header
#include "cmsis_os2.h"
#include "app.h"
#include "BLE.h"
#include "DMA.h"
#include "safe_female.h"
#include "not_female.h"
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
	APP_commState commState;
}sAPP;

void Thread_APP(void *arg)
{
	while(1)
	{
		result = osMessageQueueGet(receivedMessageQ_id, &sAPP.rxMessage, NULL, 1000);
		if(result == osOK)
		{
			osEventFlagsSet(BLE_Flags,BLE_RECEIVED_MESSAGE_TRANSFERRED);
			if(strstr((const char *)sAPP.rxMessage.dataBuffer,"UNSAFE") != NULL) //if SAFE message
			{
				asm("NOP"); //testing TODO: replace
				Control_RGB_LEDs(1,0,0);
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"SAFE") != NULL) //if UNSAFE message
			{
				asm("NOP"); //testing TODO: replace
				Control_RGB_LEDs(0,1,0);
			}
			else if(strstr((const char *)sAPP.rxMessage.dataBuffer,"ACK") != NULL) //if ACK message
			{
				asm("NOP"); //testing TODO: replace
			}
		}
		//osThreadYield();
	}
}
