#include "UART.h"
#include <stdio.h>
#include "utilities.h"

/* BEGIN - UART0 Device Driver

	Code created by Shannon Strutz
	Date : 5/7/2014
	Licensed under CC BY-NC-SA 3.0
	http://creativecommons.org/licenses/by-nc-sa/3.0/

	Modified by Alex Dean 9/13/2016
*/

static struct
{
  ISRCallback callback;
  //uint8_t *pRxBuffer; //will be passed in by calling module
	uint8_t rxCircularBuffer[UART_BUFFER_SIZE];
  //uint32_t rxBufferLength;
  uint32_t rxIndex;
	uint32_t startIndex;
} sUART;

static uint32_t UART_incrementIndex(uint32_t index, uint32_t count);
static uint32_t UART_decrementIndex(uint32_t index, uint32_t count);

extern void UART0_init(uint32_t baud_rate, ISRCallback callback)
{
  sUART.callback = callback;
  //sUART.pRxBuffer = NULL;
  //sUART.rxBufferLength = 0;
  sUART.rxIndex = 0;
  
  //the rest of the init goes here
	uint16_t sbr;
	volatile uint8_t temp;

	// Enable clock gating for UART0 and Port A
	SIM->SCGC4 |= SIM_SCGC4_UART0_MASK; 										
	SIM->SCGC5 |= SIM_SCGC5_PORTA_MASK;											

	// Make sure transmitter and receiver are disabled before init
	UART0->C2 &= ~UART0_C2_TE_MASK & ~UART0_C2_RE_MASK; 		

	// Set UART clock to 48 MHz clock 
	SIM->SOPT2 |= SIM_SOPT2_UART0SRC(1);
	SIM->SOPT2 |= SIM_SOPT2_PLLFLLSEL_MASK;

	// Set pins to UART0 Rx and Tx
	//PTA1 is Rx and PTA2 is Tx
	PORTA->PCR[1] = PORT_PCR_ISF_MASK | PORT_PCR_MUX(2); // Rx
	PORTA->PCR[2] = PORT_PCR_ISF_MASK | PORT_PCR_MUX(2); // Tx

	// Set baud rate and oversampling ratio
	sbr = (uint16_t)((SYS_CLOCK)/(baud_rate * UART_OVERSAMPLE_RATE)); 			
	UART0->BDH &= ~UART0_BDH_SBR_MASK;
	UART0->BDH |= UART0_BDH_SBR(sbr>>8);
	UART0->BDL = UART0_BDL_SBR(sbr);
	UART0->C4 |= UART0_C4_OSR(UART_OVERSAMPLE_RATE-1);				

	// Disable interrupts for RX active edge and LIN break detect, select one stop bit
	UART0->BDH |= UART0_BDH_RXEDGIE(0) | UART0_BDH_SBNS(0) | UART0_BDH_LBKDIE(0);

	// Don't enable loopback mode, use 8 data bit mode, don't use parity
	UART0->C1 = UART0_C1_LOOPS(0) | UART0_C1_M(0) | UART0_C1_PE(0); 
	// Don't invert transmit data, don't enable interrupts for errors
	UART0->C3 = UART0_C3_TXINV(0) | UART0_C3_ORIE(0)| UART0_C3_NEIE(0) 
			| UART0_C3_FEIE(0) | UART0_C3_PEIE(0);

	// Clear error flags
	UART0->S1 = UART0_S1_OR(1) | UART0_S1_NF(1) | UART0_S1_FE(1) | UART0_S1_PF(1);

	// Try it a different way
	UART0->S1 |= UART0_S1_OR_MASK | UART0_S1_NF_MASK | 
									UART0_S1_FE_MASK | UART0_S1_PF_MASK;

	// Send LSB first, do not invert received data
	UART0->S2 = UART0_S2_MSBF(0) | UART0_S2_RXINV(0); 

	NVIC_SetPriority(UART0_IRQn, 2); // 0, 1, 2, or 3
	NVIC_ClearPendingIRQ(UART0_IRQn); 
	NVIC_EnableIRQ(UART0_IRQn);

	// Enable receive interrupts but not transmit interrupts yet
	UART0->C2 |= UART_C2_RIE(1);

	// Enable UART receiver and transmitter
	UART0->C2 |= UART0_C2_RE(1) | UART0_C2_TE(1);	

	// Clear the UART RDRF flag
	temp = UART0->D;
	UART0->S1 &= ~UART0_S1_RDRF_MASK;

	//Init RX circular buffer
	Util_fillMemory(sUART.rxCircularBuffer, sizeof(sUART.rxCircularBuffer),'\0');
  sUART.rxCircularBuffer[UART_BUFFER_SIZE - 1] = '\r';
}

extern void UART_send(uint8_t *pData, uint32_t length)
{
  UART0->C2 |= UART_C2_TE(1); //enables UART transmitter
  for(uint32_t i=0;i<length;i++)
  {
    while(!(UART0->S1 & UART0_S1_TDRE_MASK));
    UART0->D = pData[i];
  }
	if(sUART.callback)
		sUART.callback(IO_WRITE); //signal to BLE (or other calling module) that we are done sending
}


extern void UART_receive(void)
{
  //sUART.rxBufferLength = length;
	sUART.startIndex = 0;
	sUART.rxIndex = 0;
	Util_fillMemory(sUART.rxCircularBuffer, UART_BUFFER_SIZE, '\0'); //clear out Rx buffer

  UART0->C2 |= UART_C2_RE(1); //enables UART Receiver
}



void UART0_IRQHandler(void)
{
	volatile uint8_t dummy_read;
  uint32_t status_reg = UART0->S1;
	
  if(status_reg & UART0_S1_RDRF_MASK) //if ISR triggered by received character
  {
    uint8_t received_val = UART0->D;
    sUART.rxCircularBuffer[sUART.rxIndex++] = received_val;
		if(sUART.rxIndex > 255) //handle wrap around
		{
			sUART.rxIndex = 0;
		}
    if(received_val == '\r') //if we are done receiving
    {
      if(sUART.callback)
        sUART.callback(IO_RECEIVE); //signal to BLE (or other calling module) that we are done receiving
    }
  }

  if(status_reg & (UART_S1_OR_MASK |UART_S1_NF_MASK |  //if some error occured
		UART_S1_FE_MASK | UART_S1_PF_MASK))
  {
	// clear the error flags
		UART0->S1 |= UART0_S1_OR_MASK | UART0_S1_NF_MASK | 
					UART0_S1_FE_MASK | UART0_S1_PF_MASK;	
		// read the data register to clear RDRF
		dummy_read = UART0->D;
    if(sUART.callback)
      sUART.callback(IO_ERROR); //signal to BLE (or other calling module) that we encountered an error
  }
}

void UART_Cancel(void){
	UART0->C2 &= ~UART_C2_RE(1); //disables UART Receiver
	//UART0->C2 &= ~UART0_C2_TE(1); //disables UART Transmitter
	Util_fillMemory(sUART.rxCircularBuffer, UART_BUFFER_SIZE, '\0'); //clear out Rx buffer
	//TODO: Also clear our Tx buffer?
}



uint8_t UART_getPacket(uint8_t *pBuffer)
{
	//the purpose of this function is to take message (string up to \r) and put it into BLE message queue
	bool packetFound=false;

	//Sanity check indexes
	if(sUART.rxCircularBuffer[UART_decrementIndex(sUART.startIndex,1)] != '\r')
	{
		//Means an overflow occured. Should never get here. If we do, increase buffer size or handle overflow somehow
		return 0;
	}
	
	uint32_t numBytesInBuffer = (UART_BUFFER_SIZE - sUART.startIndex + sUART.rxIndex)%UART_BUFFER_SIZE;
	uint32_t packetSize = 0; //declare outside of loop so we know how many bytes are in the packet
	for(packetSize = 0; packetSize < numBytesInBuffer; packetSize++)
	{
		if(sUART.rxCircularBuffer[(sUART.startIndex + packetSize)%UART_BUFFER_SIZE] == '\r')
		{
			packetFound = true;
			break;
		}
	}

	if(packetFound && (packetSize > 0))
	{
		//copy the data (size==i) to pBuffer and bump the startIndex up for the next packet.
		uint32_t len1 = packetSize;
		uint32_t len2 = 0;

		if((sUART.startIndex + packetSize) > UART_BUFFER_SIZE)
		{
			//if wrap around, we need 2 copies
			len1 = UART_BUFFER_SIZE - sUART.startIndex;
			len2 = packetSize - len1;
		}

		Util_copyMemory(&sUART.rxCircularBuffer[sUART.startIndex], pBuffer, len1);
		if(len2)
			Util_copyMemory(&sUART.rxCircularBuffer[0], &pBuffer[len1], len2);
		
		sUART.startIndex = UART_incrementIndex(sUART.startIndex, packetSize);
	}
	else{
		packetSize=0;
	}
	
	return packetSize;	
}



void UART_resetRxBuffer(void)
{
	Util_fillMemory((uint8_t*)sUART.rxCircularBuffer, UART_BUFFER_SIZE, '\0');
	sUART.startIndex=0;
	sUART.rxIndex=0;
}

static uint32_t UART_incrementIndex(uint32_t index, uint32_t count)
{
  return ((index + count) % UART_BUFFER_SIZE);  
}

static uint32_t UART_decrementIndex(uint32_t index, uint32_t count)
{
  uint32_t retIndex;
  
  if(index >= count)
    retIndex = index - count;
  else
    retIndex = UART_BUFFER_SIZE - (count - index + 1);
  
  return retIndex;
}

// *******************************ARM University Program Copyright � ARM Ltd 2013*************************************   
