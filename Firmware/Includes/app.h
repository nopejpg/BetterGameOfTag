#ifndef APP_H
#define APP_H
#include "cmsis_os2.h"

extern osThreadId_t tid_APP;
extern osMessageQueueId_t receivedMessageQ_id;
extern osEventFlagsId_t DMA_flags;
#define DMA_REC_COMPLETE 0x00000001ul

typedef enum
{
	INITIALIZING,
	DISCONNECTED,
	CONNECTED_IDLE,
	SENDING_PACKET,
	AWAITING_PACKET,
} APP_commState;


void Thread_APP(void * arg);



#endif // APP_H
