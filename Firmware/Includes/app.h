#ifndef APP_H
#define APP_H
#include "cmsis_os2.h"

extern osThreadId_t tid_APP;
extern osEventFlagsId_t DMA_flags;
#define DMA_REC_COMPLETE 0x00000001ul

void Thread_APP(void * arg);



#endif // APP_H
