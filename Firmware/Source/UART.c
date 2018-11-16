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
  ISRCallback callback_forPods;
	ISRCallback callback_forPhone;
	uint8_t rxCircularBuffer_fromPods[UART_BUFFER_SIZE];
	uint32_t rxIndex_fromPods;
	uint32_t startIndex_fromPods;
	uint8_t rxCircularBuffer_fromPhone[UART_BUFFER_SIZE];
	uint32_t rxIndex_fromPhone;
	uint32_t startIndex_fromPhone;
} sUART;

static uint32_t UART_incrementIndex(uint32_t index, uint32_t count);
static uint32_t UART_decrementIndex(uint32_t index, uint32_t count);

extern void UART0_init(uint32_t baud_rate, ISRCallback callback)
{
  sUART.callback_forPhone = callback;
  sUART.rxIndex_fromPhone = 0;
  
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
	Util_fillMemory(sUART.rxCircularBuffer_fromPhone, sizeof(sUART.rxCircularBuffer_fromPhone),'\0');
  sUART.rxCircularBuffer_fromPhone[UART_BUFFER_SIZE - 1] = '\r';
}

extern void UART1_init(uint32_t baud_rate, ISRCallback callback)
{
  sUART.callback_forPods = callback;
  sUART.rxIndex_fromPods = 0;
  
  //the rest of the init goes here
	volatile uint8_t temp;
	volatile uint8_t dummyReadToClear;

	// Enable clock gating for UART1 and Port E
	SIM->SCGC4 |= SIM_SCGC4_UART1_MASK; 										
	SIM->SCGC5 |= SIM_SCGC5_PORTE_MASK;											

	// Make sure transmitter and receiver are disabled before init
	UART1->C2 &= ~UART0_C2_TE_MASK & ~UART0_C2_RE_MASK; 		

	//UART1 uses BUS clock by default

	// Set pins to UART1 Rx and Tx
	//PTE0 is Tx and PTE1 is Rx
	PORTE->PCR[0] = PORT_PCR_ISF_MASK | PORT_PCR_MUX(3); // Tx
	PORTE->PCR[1] = PORT_PCR_ISF_MASK | PORT_PCR_MUX(3); // Rx

	// Set baud rate
	UART1->BDH &= ~UART0_BDH_SBR_MASK;
	UART1->BDH |= 0x00; //set BD to 156 to get baud rate of 9600. UART1 uses bus clock. Baud Rate = Bus Clock / (16*BR)
	UART1->BDL |= 0x9C;
	
	UART1->C4 |= UART0_C4_OSR(UART_OVERSAMPLE_RATE-1);				

	// Disable interrupts for RX active edge and LIN break detect, select one stop bit
	UART1->BDH |= UART0_BDH_RXEDGIE(0) | UART0_BDH_SBNS(0) | UART0_BDH_LBKDIE(0);

	// Don't enable loopback mode, use 8 data bit mode, don't use parity
	UART1->C1 = UART0_C1_LOOPS(0) | UART0_C1_M(0) | UART0_C1_PE(0); 
	// Don't invert transmit data, don't enable interrupts for errors
	UART1->C3 = UART0_C3_TXINV(0) | UART0_C3_ORIE(0)| UART0_C3_NEIE(0) 
			| UART0_C3_FEIE(0) | UART0_C3_PEIE(0);

	// Clear error flags
	//UART1->S1 = UART0_S1_OR(1) | UART0_S1_NF(1) | UART0_S1_FE(1) | UART0_S1_PF(1);
	dummyReadToClear = UART1->S1;
	dummyReadToClear = UART1_D;

	// Try it a different way
	//UART0->S1 |= UART0_S1_OR_MASK | UART0_S1_NF_MASK | 
	//								UART0_S1_FE_MASK | UART0_S1_PF_MASK;

	// Send LSB first, do not invert received data
	UART1->S2 = UART0_S2_MSBF(0) | UART0_S2_RXINV(0); 

	NVIC_SetPriority(UART1_IRQn, 2); // 0, 1, 2, or 3
	NVIC_ClearPendingIRQ(UART1_IRQn); 
	NVIC_EnableIRQ(UART1_IRQn);

	// Enable receive interrupts but not transmit interrupts yet
	UART1->C2 |= UART_C2_RIE(1);

	// Enable UART receiver and transmitter
	UART1->C2 |= UART0_C2_RE(1) | UART0_C2_TE(1);	

	// Clear the UART RDRF flag
	//temp = UART0->D;
	//UART0->S1 &= ~UART0_S1_RDRF_MASK;
	dummyReadToClear = UART1->S1;
	dummyReadToClear = UART1_D;

	//Init RX circular buffer
	Util_fillMemory(sUART.rxCircularBuffer_fromPods, sizeof(sUART.rxCircularBuffer_fromPods),'\0');
  sUART.rxCircularBuffer_fromPods[UART_BUFFER_SIZE - 1] = '\r';
}

extern void UART_send(uint8_t *pData, uint32_t length, uint8_t UART_num)
{
	
	if(UART_num == 0)
	{
		UART0->C2 |= UART_C2_TE(1); //enables UART transmitter
		for(uint32_t i=0;i<length;i++)
		{
			while(!(UART0->S1 & UART0_S1_TDRE_MASK));
			UART0->D = pData[i];
		}
		if(sUART.callback_forPhone)
			sUART.callback_forPhone(IO_WRITE); //signal to BLE (or other calling module) that we are done sending
	}
	else
	{
		UART1->C2 |= UART_C2_TE(1); //enables UART transmitter
		for(uint32_t i=0;i<length;i++)
		{
			while(!(UART1->S1 & UART0_S1_TDRE_MASK));
			UART1->D = pData[i];
		}
		if(sUART.callback_forPods)
			sUART.callback_forPods(IO_WRITE); //signal to BLE (or other calling module) that we are done sending
	}
}


extern void UART_receive(void)
{
	sUART.startIndex_fromPods = 0;
	sUART.startIndex_fromPhone = 0;
	sUART.rxIndex_fromPods = 0;
	sUART.rxIndex_fromPhone = 0;
	Util_fillMemory(sUART.rxCircularBuffer_fromPods, UART_BUFFER_SIZE, '\0'); //clear out Rx buffer
	Util_fillMemory(sUART.rxCircularBuffer_fromPhone, UART_BUFFER_SIZE, '\0'); //clear out Rx buffer

  UART0->C2 |= UART_C2_RE(1); //enables UART Receiver
	UART1->C2 |= UART_C2_RE(1); //enables UART Receiver
}


void UART0_IRQHandler(void)
{
	volatile uint8_t dummy_read;
  uint32_t status_reg = UART0->S1;
	
  if(status_reg & UART0_S1_RDRF_MASK) //if ISR triggered by received character
  {
    uint8_t received_val = UART0->D;
    sUART.rxCircularBuffer_fromPhone[sUART.rxIndex_fromPhone++] = received_val;
		if(sUART.rxIndex_fromPhone > 255) //handle wrap around
		{
			sUART.rxIndex_fromPhone = 0;
		}
    if(received_val == '\r') //if we are done receiving
    {
      if(sUART.callback_forPhone)
        sUART.callback_forPhone(IO_RECEIVE); //signal to BLE (or other calling module) that we are done receiving
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
    if(sUART.callback_forPhone)
      sUART.callback_forPhone(IO_ERROR); //signal to BLE (or other calling module) that we encountered an error
  }
}

void UART1_IRQHandler(void)
{
	volatile uint8_t dummy_read;
  uint32_t status_reg = UART1->S1;
	
  if(status_reg & UART0_S1_RDRF_MASK) //if ISR triggered by received character
  {
    uint8_t received_val = UART1->D;
    sUART.rxCircularBuffer_fromPods[sUART.rxIndex_fromPods++] = received_val;
		if(sUART.rxIndex_fromPods > 255) //handle wrap around
		{
			sUART.rxIndex_fromPods = 0;
		}
    if(received_val == '\r') //if we are done receiving
    {
      if(sUART.callback_forPods)
        sUART.callback_forPods(IO_RECEIVE); //signal to BLE (or other calling module) that we are done receiving
    }
  }

  if(status_reg & (UART_S1_OR_MASK |UART_S1_NF_MASK |  //if some error occured
		UART_S1_FE_MASK | UART_S1_PF_MASK))
  {
	// clear the error flags
		dummy_read = UART1->S1;
		dummy_read = UART1_D;
		// read the data register to clear RDRF
		dummy_read = UART1->D;
    if(sUART.callback_forPods)
      sUART.callback_forPods(IO_ERROR); //signal to BLE (or other calling module) that we encountered an error
  }
}




void UART_Cancel(void){
	UART0->C2 &= ~UART_C2_RE(1); //disables UART Receiver
	UART1->C2 &= ~UART_C2_RE(1); //disables UART Receiver
	Util_fillMemory(sUART.rxCircularBuffer_fromPods, UART_BUFFER_SIZE, '\0'); //clear out Rx buffer
	Util_fillMemory(sUART.rxCircularBuffer_fromPhone, UART_BUFFER_SIZE, '\0'); //clear out Rx buffer
}


uint8_t UART_getPacket_fromPhone(uint8_t *pBuffer)
{
	//the purpose of this function is to take message (string up to \r) and put it into BLE message queue
	bool packetFound=false;

	//Sanity check indexes
	if(sUART.rxCircularBuffer_fromPhone[UART_decrementIndex(sUART.startIndex_fromPhone,1)] != '\r')
	{
		//Means an overflow occured. Should never get here. If we do, increase buffer size or handle overflow somehow
		return 0;
	}
	
	uint32_t numBytesInBuffer = (UART_BUFFER_SIZE - sUART.startIndex_fromPhone + sUART.rxIndex_fromPhone)%UART_BUFFER_SIZE;
	uint32_t packetSize = 0; //declare outside of loop so we know how many bytes are in the packet
	for(packetSize = 0; packetSize < numBytesInBuffer; packetSize++)
	{
		if(sUART.rxCircularBuffer_fromPhone[(sUART.startIndex_fromPhone + packetSize)%UART_BUFFER_SIZE] == '\r')
		{
			packetFound = true;
			packetSize++; //increment here, because it does not increment before breaking out of loop.
			break;
		}
	}

	if(packetFound && (packetSize > 0))
	{
		//copy the data (size==i) to pBuffer and bump the startIndex up for the next packet.
		uint32_t len1 = packetSize;
		uint32_t len2 = 0;

		if((sUART.startIndex_fromPhone + packetSize) > UART_BUFFER_SIZE)
		{
			//if wrap around, we need 2 copies
			len1 = UART_BUFFER_SIZE - sUART.startIndex_fromPhone;
			len2 = packetSize - len1;
		}

		Util_copyMemory(&sUART.rxCircularBuffer_fromPhone[sUART.startIndex_fromPhone], pBuffer, len1);
		if(len2)
			Util_copyMemory(&sUART.rxCircularBuffer_fromPhone[0], &pBuffer[len1], len2);
		
		sUART.startIndex_fromPhone = UART_incrementIndex(sUART.startIndex_fromPhone, packetSize);
	}
	else{
		packetSize=0;
	}
	
	return packetSize;	
}


uint8_t UART_getPacket_fromPods(uint8_t *pBuffer)
{
	//the purpose of this function is to take message (string up to \r) and put it into BLE message queue
	bool packetFound=false;

	//Sanity check indexes
	if(sUART.rxCircularBuffer_fromPods[UART_decrementIndex(sUART.startIndex_fromPods,1)] != '\r')
	{
		//Means an overflow occured. Should never get here. If we do, increase buffer size or handle overflow somehow
		return 0;
	}
	
	uint32_t numBytesInBuffer = (UART_BUFFER_SIZE - sUART.startIndex_fromPods + sUART.rxIndex_fromPods)%UART_BUFFER_SIZE;
	uint32_t packetSize = 0; //declare outside of loop so we know how many bytes are in the packet
	for(packetSize = 0; packetSize < numBytesInBuffer; packetSize++)
	{
		if(sUART.rxCircularBuffer_fromPods[(sUART.startIndex_fromPods + packetSize)%UART_BUFFER_SIZE] == '\r')
		{
			packetFound = true;
			packetSize++; //increment here, because it does not increment before breaking out of loop.
			break;
		}
	}

	if(packetFound && (packetSize > 0))
	{
		//copy the data (size==i) to pBuffer and bump the startIndex up for the next packet.
		uint32_t len1 = packetSize;
		uint32_t len2 = 0;

		if((sUART.startIndex_fromPods + packetSize) > UART_BUFFER_SIZE)
		{
			//if wrap around, we need 2 copies
			len1 = UART_BUFFER_SIZE - sUART.startIndex_fromPods;
			len2 = packetSize - len1;
		}

		Util_copyMemory(&sUART.rxCircularBuffer_fromPods[sUART.startIndex_fromPods], pBuffer, len1);
		if(len2)
			Util_copyMemory(&sUART.rxCircularBuffer_fromPods[0], &pBuffer[len1], len2);
		
		sUART.startIndex_fromPods = UART_incrementIndex(sUART.startIndex_fromPods, packetSize);
	}
	else{
		packetSize=0;
	}
	
	return packetSize;	
}



void UART_resetRxBuffer(void)
{
	Util_fillMemory((uint8_t*)sUART.rxCircularBuffer_fromPhone, UART_BUFFER_SIZE, '\0');
	sUART.rxCircularBuffer_fromPhone[UART_BUFFER_SIZE - 1] = '\r';
	sUART.startIndex_fromPhone=0;
	sUART.rxIndex_fromPhone=0;
	
	Util_fillMemory((uint8_t*)sUART.rxCircularBuffer_fromPods, UART_BUFFER_SIZE, '\0');
	sUART.rxCircularBuffer_fromPods[UART_BUFFER_SIZE - 1] = '\r';
	sUART.startIndex_fromPods=0;
	sUART.rxIndex_fromPods=0;
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
		retIndex = UART_BUFFER_SIZE - (count - index);
  
  return retIndex;
}

// *******************************ARM University Program Copyright � ARM Ltd 2013*************************************   
