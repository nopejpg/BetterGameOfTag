/*----------------------------------------------------------------------------
 * CMSIS-RTOS 'main' function template
 *---------------------------------------------------------------------------*/
#include <MKL25Z4.H>
#include "RTE_Components.h"
#include  CMSIS_device_header
#include "cmsis_os2.h"

#include "threads.h"
#include "BLE.h"
#include "DMA.h"
#include "DAC.h"


#ifdef RTE_Compiler_EventRecorder
#include "EventRecorder.h"
#endif
 
/*----------------------------------------------------------------------------
 * Application main thread
 *---------------------------------------------------------------------------*/
 
int main (void) {
 
  // System Initialization
  SystemCoreClockUpdate();
#ifdef RTE_Compiler_EventRecorder
  // Initialize and start Event Recorder
  EventRecorderInitialize(EventRecordError, 1U);
#endif
  // ...
	BLE_init();
	Init_DMA_For_Playback();
	Init_DAC();
	
  osKernelInitialize();                 // Initialize CMSIS-RTOS
	tid_TEST = osThreadNew(Thread_Testing, NULL, NULL);    // Create thread
	DMA_flags = osEventFlagsNew(NULL);
  osKernelStart();                      // Start thread execution
  for (;;) {}
}
