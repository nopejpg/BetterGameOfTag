#include "timers.h"
#include "MKL25Z4.h"
#include "queue.h"

extern Q_T queue;

void Init_TPM(uint32_t period_us)
{
	//turn on clock to TPM 
	SIM->SCGC6 |= SIM_SCGC6_TPM0_MASK;
	
	//set clock source for tpm (PLL output / 2 = 24MHz)
	SIM->SOPT2 |= (SIM_SOPT2_TPMSRC(1) | SIM_SOPT2_PLLFLLSEL_MASK);

	// disable TPM
	TPM0->SC = 0;
	
	//load the counter and mod (interrupt will fire every period_us microseconds)
	//if want to fire at 8kHz, need to make period_us 125
	TPM0->MOD = TPM_MOD_MOD(period_us*24);

	//set TPM to count up and divide by 2 prescaler and clock mode
	TPM0->SC = (TPM_SC_DMA_MASK | TPM_SC_PS(1));
	
	TPM0->SC |= TPM_SC_TOIE_MASK;

	// Configure NVIC 
	NVIC_SetPriority(TPM0_IRQn, 128); // 0, 64, 128 or 192
	NVIC_ClearPendingIRQ(TPM0_IRQn); 
	NVIC_EnableIRQ(TPM0_IRQn);	
}

void Start_TPM(void) {
// Enable counter
	TPM0->SC |= TPM_SC_CMOD(1);
}

void TPM0_IRQHandler() {
	//clear pending IRQ
	NVIC_ClearPendingIRQ(TPM0_IRQn);
	
	TPM0->SC |= TPM_SC_TOF_MASK; 
}

// *******************************ARM University Program Copyright © ARM Ltd 2013*************************************   
