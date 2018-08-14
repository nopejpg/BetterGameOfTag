#ifndef BLE_H
#define BLE_H

#include <stdint.h>
#include <stdbool.h>
#include "cmsis_os2.h"

#define BLE_MESSAGE_RECEIVED (1UL << 0)
#define BLE_TRANSMIT_SUCCESS (1UL << 1)
#define BLE_EXPECTING_RESPONSE (1UL << 2)
#define BLE_MESSAGE_READY_FOR_TRANSMIT (1UL << 3)
#define BLE_RECEIVED_MESSAGE_TRANSFERRED (1U << 4) //used for transferring data to app thread
#define BLE_ACK_RECEIVED (1U << 5) //used for retries

#define BLE_INIT_AND_CONNECTED (1UL << 0) //used for thread flag (not part of the above event flag defines)


#define PACKET_START_BYTE 0xFD
#define PACKET_SIZE 128
#define MESSAGE_SIZE 128
#define COMMS_MAX_RETRIES 3

#define BLE_ADDRESS_LENGTH 12

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

extern osEventFlagsId_t BLE_Flags;
extern osThreadId_t tid_BLE;
								 
void Thread_BLE(void * arg);								 

void BLE_init(void);
void BLE_connectToDevice(const char *pBTAddress);
void BLE_SendCommand(const char *pString);
void BLE_SendAck(void);
enum opResult BLE_stdCommand(uint8_t *command);

#endif
