#ifndef THREADS_H
#define THREADS_H
#include "cmsis_os2.h"

extern osThreadId_t tid_BLE;
extern osThreadId_t tid_APP;
extern osEventFlagsId_t DMA_flags;
#define DMA_REC_COMPLETE 0x00000001ul

void Thread_APP(void * arg);
void Thread_BLE(void * arg);



#endif // THREADS_H
