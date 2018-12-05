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
 
 const osThreadAttr_t Thread_BLE_attr = {
		.priority = osPriorityAboveNormal,
		.stack_size = 512,
	};
 
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
	//Init_My_LEDs(48000);
	Init_My_Red_LED();
	Init_My_Green_LED_PWM(TPM_PERIOD);
	Init_My_Blue_LED();
	Control_RGB_LEDs(0,0,0);
	
  osKernelInitialize();                 // Initialize CMSIS-RTOS
#ifdef IS_HUB_DEVICE
	tid_APP = osThreadNew(Thread_APP_HUB, NULL, NULL);    // Create hub's app thread
#else
	tid_APP = osThreadNew(Thread_APP_POD, NULL, NULL);    // Create pod's app thread
#endif
	
	tid_BLE = osThreadNew(Thread_BLE, NULL, &Thread_BLE_attr);    // Create thread
	
	
	DMA_flags = osEventFlagsNew(NULL);
  osKernelStart();                      // Start thread execution
  for (;;) {}
}
