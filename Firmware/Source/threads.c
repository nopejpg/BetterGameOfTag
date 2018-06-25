#include "threads.h"
#include "RTE_Components.h"
#include CMSIS_device_header
#include "cmsis_os2.h"
#include "UART.h"
#include "DMA.h"
#include "safe_female.h"
#include "not_female.h"

osThreadId_t tid_TEST;
osEventFlagsId_t DMA_flags;

void Thread_Testing(void * arg) {
	while (1) {
		//Send_String((uint8_t*)"TEST");
		Send_String("TEST");
		Play_Recording(safe_female, 4376);
		//waits until recording complete, then clears flag
		osEventFlagsWait(DMA_flags,DMA_REC_COMPLETE, osFlagsWaitAll, osWaitForever);
	}
}
