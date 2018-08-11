#ifndef BLE_H
#define BLE_H

#include <stdint.h>
#include <stdbool.h>
#include "cmsis_os2.h"

#define BLE_MESSAGE_RECEIVED (1UL << 0)
#define BLE_TRANSMIT_SUCCESS (1UL << 1)
#define BLE_EXPECTING_RESPONSE (1UL << 2)
#define BLE_MESSAGE_READY_FOR_TRANSMIT (1UL << 3)


#define PACKET_START_BYTE 0xFD
#define PACKET_SIZE 128
#define MESSAGE_SIZE 128

#define BLE_ADDRESS_LENGTH 12

enum opResult {REMOTE_ERROR = -5, CONNECT_ERROR, INVALID_PARAM,
                 TIMEOUT_ERROR, MODULE_ERROR, DEFAULT_ERR, SUCCESS};

typedef enum
{
	SENDING_COMMAND,
	AWAITING_RESPONSE,
	PROCESSING_RESPONSE,
	IDLE
} BLE_moduleState;

extern osEventFlagsId_t BLE_Flags;
								 
								 
void BLE_init(void);
void BLE_SendPacket(const char *pString);
void BLE_connectToDevice(const char *pBTAddress);
enum opResult BLE_Advertise(void);
enum opResult BLE_noAdvertise(void);
enum opResult BLE_stdCommand(uint8_t *command);

#endif
