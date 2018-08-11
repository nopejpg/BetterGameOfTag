#ifndef UART_H
#define UART_H

#include <stdint.h>
#include <MKL25Z4.H>

#define UART_OVERSAMPLE_RATE 	(16)
#define BUS_CLOCK 						(24e6)
#define SYS_CLOCK							(48e6)

#define UART_BUFFER_SIZE 256

#include <stdint.h>
#include <stdbool.h>

typedef enum
{
  IO_RECEIVE,
  IO_WRITE,
  IO_ERROR
} UARTOperationEnum;


typedef void (*ISRCallback)(UARTOperationEnum op);

extern void UART0_init(uint32_t baud_rate, ISRCallback callback);
extern void UART_send(uint8_t *pData, uint32_t length);
extern void UART_receive(void);
extern void UART_resetRxBuffer(void);
//extern void UART_stop_receiving(void);
extern uint8_t UART_getPacket(uint8_t *pBuffer);

void UART_Cancel(void);

#endif
// *******************************ARM University Program Copyright � ARM Ltd 2013*************************************   
