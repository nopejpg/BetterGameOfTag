#ifndef THREADS_H
#define THREADS_H
#include "cmsis_os2.h"

extern osThreadId_t tid_TEST;
extern osEventFlagsId_t DMA_flags;
#define DMA_REC_COMPLETE 0x00000001ul

void Thread_Testing(void * arg);



#endif // THREADS_H
