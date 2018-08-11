#include "threads.h"
#include "RTE_Components.h"
#include CMSIS_device_header
#include "cmsis_os2.h"
#include "BLE.h"
#include "DMA.h"
#include "safe_female.h"
#include "not_female.h"
#include "LEDs.h"

osThreadId_t tid_APP;

osEventFlagsId_t DMA_flags;

void Thread_APP(void *arg)
{
	while(1)
	{
		//TODO
		osThreadYield();
	}
}
