#ifndef APP_H
#define APP_H
#include "cmsis_os2.h"

extern osThreadId_t tid_APP;
extern osMessageQueueId_t receivedMessageQ_id;
extern osMessageQueueId_t deviceConnectionRequestQ_id;
extern osMessageQueueId_t requestedPodStatesQ_id;
extern osMessageQueueId_t podStateRequestResultsQ_id;
extern osEventFlagsId_t DMA_flags;
extern osEventFlagsId_t APP_Request_Flags;
extern osTimerId_t APP_AutoTagTimer_id;
#define DMA_REC_COMPLETE 0x00000001ul

/*Event flags used to tell the BLE class/module what the APP class/module wants it to do*/
#define APP_CONNECT_TO_POD (1UL << 0) //tells BLE thread that the APP thread wants to connect to a pod
#define APP_REQUEST_COMPLETE (1UL << 1) //tells APP thread that BLE thread is done processing its request
#define APP_FIND_PHONE_ADDRESS (1UL << 2)
#define APP_WAIT_FOR_PODS (1UL << 3)
#define APP_CONNECT_TO_PHONE (1UL << 4)
#define APP_CHANGE_POD_STATES (1UL << 5)
#define APP_SEND_ACK (1UL << 6)
#define APP_ENTER_CENTRAL_MODE (1UL << 7)
#define APP_ENTER_PERIPHERAL_MODE (1UL << 8)
#define APP_AUTO_TAG_CHANGE_TIMER_EXPIRED (1UL << 9)
#define APP_MESSAGE_PENDING_FROM_BLE (1UL << 10)
#define APP_AUTO_TAG_WARNING_TIMER_EXPIRED (1UL << 11)

#ifdef IS_HUB_DEVICE
void Thread_APP_HUB(void * arg);
#define NUM_PODS (3)
#else //if this is a pod device
void Thread_APP_POD(void * arg);
#endif




#endif // APP_H
