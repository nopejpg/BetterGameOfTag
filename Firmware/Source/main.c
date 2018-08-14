/*----------------------------------------------------------------------------
 * CMSIS-RTOS 'main' function template
 *---------------------------------------------------------------------------*/
#include <MKL25Z4.H>
#include "RTE_Components.h"
#include  CMSIS_device_header
#include "cmsis_os2.h"

#include "app.h"
#include "BLE.h"
#include "DMA.h"
#include "DAC.h"
#include "LEDs.h"


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
	Init_DMA_For_Playback();
	Init_DAC();
	Init_RGB_LEDs();
	Control_RGB_LEDs(0,0,0);
	
  osKernelInitialize();                 // Initialize CMSIS-RTOS
	tid_APP = osThreadNew(Thread_APP, NULL, NULL);    // Create thread
	tid_BLE = osThreadNew(Thread_BLE, NULL, NULL);    // Create thread
	//receivedMessageQ_id = osMessageQueueNew(1,sizeof(uint8_t*),NULL);
	
	
	DMA_flags = osEventFlagsNew(NULL);
  osKernelStart();                      // Start thread execution
  for (;;) {}
}
