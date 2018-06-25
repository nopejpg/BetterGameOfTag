#ifndef UART_H
#define UART_H

#include <stdint.h>
#include <MKL25Z4.H>
#include "queue.h"

#define UART_OVERSAMPLE_RATE 	(16)
#define BUS_CLOCK 						(24e6)
#define SYS_CLOCK							(48e6)

void Init_UART0(uint32_t baud_rate);

void Send_String(uint8_t * str);

extern Q_T TxQ, RxQ;

#endif
// *******************************ARM University Program Copyright © ARM Ltd 2013*************************************   
