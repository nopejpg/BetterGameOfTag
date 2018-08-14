#ifndef APP_H
#define APP_H
#include "cmsis_os2.h"

extern osThreadId_t tid_APP;
extern osMessageQueueId_t receivedMessageQ_id;
extern osEventFlagsId_t DMA_flags;
#define DMA_REC_COMPLETE 0x00000001ul


#ifdef IS_HUB_DEVICE
void Thread_APP_HUB(void * arg);
#else //if this is a pod device
void Thread_APP_POD(void * arg);
#endif




#endif // APP_H
