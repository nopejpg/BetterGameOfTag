#ifndef BLE_H
#define BLE_H

#include <stdint.h>
#include <stdbool.h>
#include "cmsis_os2.h"
#include <stdio.h>

/*BLE Module Flags Below (used for synchronizing events within the BLE class/module*/
#define BLE_MESSAGE_RECEIVED_FROM_PHONE (1UL << 0)
#define BLE_TRANSMIT_SUCCESS (1UL << 1)
#define BLE_EXPECTING_RESPONSE (1UL << 2)
#define BLE_MESSAGE_READY_FOR_TRANSMIT (1UL << 3)
#define BLE_RECEIVED_MESSAGE_TRANSFERRED (1U << 4) //used for transferring data to app thread
#define BLE_ACK_RECEIVED (1U << 5) //used for retries
#define BLE_CONNECTED_TO_PHONE (1U << 6) //used for BLE_connectToDevice()
#define BLE_CONNECTED_TO_POD (1U << 7) //used for BLE_connectToDevice()
#define APP_THREAD_REQESTING_ACTION (1U << 8) //used to tell BLE that the app has a request. Below flags provide specifics
#define BLE_MELODYSMART_ADDRESS_FOUND (1U << 9) //used to let us know that a potential phone address has been found
#define BLE_TEMP_STOP_CON_ATTEMPTS (1U <<  10) //used to tell ourselves that we need to run function to momentarily stop connection attempts to hub
#define BLE_MESSAGE_RECEIVED_FROM_PODS (1UL << 11)

/*Flag(s) used to synchronize APP and BLE threads*/
#define BLE_INIT_AND_CONNECTED (1UL << 0) //tells APP thread that BLE is initialized and ready for use


#define PACKET_START_BYTE 0xFD
#define PACKET_SIZE 128
#define MESSAGE_SIZE 128
#define COMMS_MAX_RETRIES 3

#define BLE_ADDRESS_LENGTH 12
#define POD1 (1)
#define POD2 (2)
#define POD3 (3)

#define SEND_TO_PHONE (0)
#define SEND_TO_POD (1)

#define PHONE_BLE (0)
#define PODS_BLE (1)

enum opResult {REMOTE_ERROR = -5, CONNECT_ERROR, INVALID_PARAM,
                 TIMEOUT_ERROR, MODULE_ERROR, DEFAULT_ERR, SUCCESS};

typedef enum
{
	SENDING_COMMAND,
	AWAITING_RESPONSE,
	RESPONSE_RECEIVED,
	IDLE,
	ERROR_STATE
} BLE_moduleState;

typedef enum
{
	INITIALIZING,
	DISCONNECTED,
	CONNECTED_IDLE,
	SENDING_PACKET,
	AWAITING_ACK,
	ACK_RECEIVED
} BLE_commState;

typedef enum
{
	INIT,
	//states for tag games
	SAFE,
	UNSAFE,
	WARNING,
	REMAIN_SAME,
	//states for RL_GL game
	OFF,
	RUN,
	WALK,
	STOP
}podStates;


extern osEventFlagsId_t BLE_Flags;
extern osThreadId_t tid_BLE;
								 
void Thread_BLE(void * arg);								 

void BLE_init(void);
bool BLE_checkConnectedStatus(void);
enum opResult BLE_stdCommand(uint8_t *command, uint8_t UART_num);

#endif
