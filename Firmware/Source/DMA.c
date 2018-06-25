#include "DMA.h"
#include <MKL25Z4.h>
#include "timers.h"
#include "threads.h"

static const char * Reload_DMA_Source=0;
static uint32_t Reload_DMA_Byte_Count=0;
//volatile uint8_t recording_complete=0; //TODO: Make this NOT a global. use RTOS method

void Init_DMA(void) {
	// Gate clocks to DMA and DMAMUX
	SIM->SCGC7 |= SIM_SCGC7_DMA_MASK;
	SIM->SCGC6 |= SIM_SCGC6_DMAMUX_MASK;

	// Disable DMA channel to allow configuration
	DMAMUX0->CHCFG[0] = 0;
	
	// Generate DMA interrupt when done
	// Increment source, transfer words (8 bits)
	// Enable peripheral request
	DMA0->DMA[0].DCR = DMA_DCR_EINT_MASK | DMA_DCR_SINC_MASK | 
											DMA_DCR_SSIZE(1) | DMA_DCR_DSIZE(1) |
											DMA_DCR_ERQ_MASK | DMA_DCR_CS_MASK;
	
	// Configure NVIC for DMA ISR
	NVIC_SetPriority(DMA0_IRQn, 2);
	NVIC_ClearPendingIRQ(DMA0_IRQn); 
	NVIC_EnableIRQ(DMA0_IRQn);	

	// Enable DMA MUX channel with TPM0 overflow as trigger
	DMAMUX0->CHCFG[0] = DMAMUX_CHCFG_SOURCE(54);   
}

void Init_DMA_For_Playback(void) {
	Init_DMA();
	Init_TPM(125);
	Start_TPM();
}

void DMA0_IRQHandler(void) {
	//disable DMA until next Play_Recording() call
	DMAMUX0->CHCFG[0] &= ~DMAMUX_CHCFG_ENBL_MASK; 
	osEventFlagsSet(DMA_flags, DMA_REC_COMPLETE);
	// Clear done flag 
	DMA0->DMA[0].DSR_BCR |= DMA_DSR_BCR_DONE_MASK; 
}

void Play_Recording(const char * source, uint32_t count){
	// Save reload information
	Reload_DMA_Source = source;
	//Reload_DMA_Byte_Count = count*2;
	Reload_DMA_Byte_Count = count;
	
	// initialize source and destination pointers
	DMA0->DMA[0].SAR = DMA_SAR_SAR((uint32_t) Reload_DMA_Source);
	DMA0->DMA[0].DAR = DMA_DAR_DAR((uint32_t) (&(DAC0->DAT[0])));
	//DMA0->DMA[0].DAR = DMA_DAR_DAR((uint32_t) (&(DAC0->DAT[0].DATH)));
	// byte count
	DMA0->DMA[0].DSR_BCR = DMA_DSR_BCR_BCR(Reload_DMA_Byte_Count);
	// clear done flag 
	DMA0->DMA[0].DSR_BCR &= ~DMA_DSR_BCR_DONE_MASK; 
	// set enable flag
	DMAMUX0->CHCFG[0] |= DMAMUX_CHCFG_ENBL_MASK;
}
