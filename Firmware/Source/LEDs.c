#include <MKL25Z4.H>
#include "LEDs.h"



void Init_RGB_LEDs(void) {
	// Enable clock to ports B and D
	SIM->SCGC5 |= SIM_SCGC5_PORTB_MASK | SIM_SCGC5_PORTD_MASK;;
	
	// Make 3 pins GPIO
	PORTB->PCR[RED_LED_POS] &= ~PORT_PCR_MUX_MASK;          
	PORTB->PCR[RED_LED_POS] |= PORT_PCR_MUX(1);          
	PORTB->PCR[GREEN_LED_POS] &= ~PORT_PCR_MUX_MASK;          
	PORTB->PCR[GREEN_LED_POS] |= PORT_PCR_MUX(1);          
	PORTD->PCR[BLUE_LED_POS] &= ~PORT_PCR_MUX_MASK;          
	PORTD->PCR[BLUE_LED_POS] |= PORT_PCR_MUX(1);          
	
	// Set ports to outputs
	PTB->PDDR |= MASK(RED_LED_POS) | MASK(GREEN_LED_POS);
	PTD->PDDR |= MASK(BLUE_LED_POS);
}

void Control_RGB_LEDs(uint8_t red_on, uint8_t green_on, uint8_t blue_on) {
	if (red_on) {
			PTB->PCOR = MASK(RED_LED_POS);
	} else {
			PTB->PSOR = MASK(RED_LED_POS); 
	}
	if (green_on) {
			PTB->PCOR = MASK(GREEN_LED_POS);
	}	else {
			PTB->PSOR = MASK(GREEN_LED_POS); 
	} 
	if (blue_on) {
			PTD->PCOR = MASK(BLUE_LED_POS);
	}	else {
			PTD->PSOR = MASK(BLUE_LED_POS); 
	}
}

void Init_My_Blue_LED(void)
{
	SIM->SCGC5 |= SIM_SCGC5_PORTB_MASK;
	// Make 3 pins GPIO
	PORTB->PCR[MY_BLUE_LED_POS] &= ~PORT_PCR_MUX_MASK;          
	PORTB->PCR[MY_BLUE_LED_POS] |= PORT_PCR_MUX(1);                  
	
	// Set ports to outputs
	PTB->PDDR |= MASK(MY_BLUE_LED_POS);
}

void Init_My_Green_LED_PWM(uint16_t period){
//	SIM->SCGC5 |= SIM_SCGC5_PORTB_MASK;
//	// Make 3 pins GPIO
//	PORTB->PCR[MY_RED_LED_POS] &= ~PORT_PCR_MUX_MASK;          
//	PORTB->PCR[MY_RED_LED_POS] |= PORT_PCR_MUX(1);          
//	PORTB->PCR[MY_GREEN_LED_POS] &= ~PORT_PCR_MUX_MASK;          
//	PORTB->PCR[MY_GREEN_LED_POS] |= PORT_PCR_MUX(1);          
//	PORTB->PCR[MY_BLUE_LED_POS] &= ~PORT_PCR_MUX_MASK;          
//	PORTB->PCR[MY_BLUE_LED_POS] |= PORT_PCR_MUX(1);          
//	
//	// Set ports to outputs
//	PTB->PDDR |= MASK(MY_RED_LED_POS) | MASK(MY_GREEN_LED_POS) | MASK(MY_BLUE_LED_POS);
	
	// Enable clock to port D
	SIM->SCGC5 |= SIM_SCGC5_PORTB_MASK;
	
	// Blue FTM0_CH1, Mux Alt 4
	// Set pin to FTMs
	PORTB->PCR[MY_GREEN_LED_POS] &= ~PORT_PCR_MUX_MASK;          
	PORTB->PCR[MY_GREEN_LED_POS] |= PORT_PCR_MUX(3);          

	// Configure TPM
	SIM->SCGC6 |= SIM_SCGC6_TPM1_MASK;
	//set clock source for tpm: 48 MHz
	SIM->SOPT2 |= (SIM_SOPT2_TPMSRC(1) | SIM_SOPT2_PLLFLLSEL_MASK);
	//load the counter and mod
	TPM1->MOD = period-1;  
	//set TPM count direction to up with a divide by 2 prescaler 
	TPM1->SC =  TPM_SC_PS(1);
	// Continue operation in debug mode
	TPM1->CONF |= TPM_CONF_DBGMODE(3);
	// Set channel 1 to edge-aligned low-true PWM
	TPM1->CONTROLS[1].CnSC = TPM_CnSC_MSB_MASK | TPM_CnSC_ELSA_MASK;
	// Set initial duty cycle as off
	TPM1->CONTROLS[1].CnV = period;
	// Start TPM
	TPM1->SC |= TPM_SC_CMOD(1);
	
}

void Init_My_Red_LED(void){
	SIM->SCGC5 |= SIM_SCGC5_PORTB_MASK;
	// Make 3 pins GPIO
	PORTB->PCR[MY_RED_LED_POS] &= ~PORT_PCR_MUX_MASK;          
	PORTB->PCR[MY_RED_LED_POS] |= PORT_PCR_MUX(1);                  
	
	// Set ports to outputs
	PTB->PDDR |= MASK(MY_RED_LED_POS);
	
}

void Control_My_LEDs(unsigned int red_on, unsigned int green_on, unsigned int blue_on){
	//TPM1->CONTROLS[1].CnV = 42000;
		//TPM2->CONTROLS[0].CnV = 0; //full on blue
	
	if (red_on) {
		PTB->PSOR = MASK(MY_RED_LED_POS);
	} else {
		PTB->PCOR = MASK(MY_RED_LED_POS);
	}
	if (green_on && red_on) //if yellow
	{
		TPM1->CONTROLS[1].CnV = 30000; //turn down green to make yellow when mixed with red
	}
	else if(green_on)
	{
		TPM1->CONTROLS[1].CnV = 0; //full on
	}
	else
	{
		TPM1->CONTROLS[1].CnV = TPM_PERIOD; //full off
	}
	if (blue_on) {
		PTB->PSOR = MASK(MY_BLUE_LED_POS);
	}	else {
		PTB->PCOR = MASK(MY_BLUE_LED_POS);
	}
}

// *******************************ARM University Program Copyright © ARM Ltd 2013*************************************   
