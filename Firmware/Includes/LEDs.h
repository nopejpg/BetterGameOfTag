#ifndef LEDS_H
#define LEDS_H
#include <MKL25Z4.H>

// Freedom KL25Z LEDs
#define RED_LED_POS (18)		// on port B
#define GREEN_LED_POS (19)	// on port B
#define BLUE_LED_POS (1)		// on port D

#define MY_RED_LED_POS (3)  // on port B
#define MY_GREEN_LED_POS (1)  // on port B
#define MY_BLUE_LED_POS (2)	// on port B

#define MASK(x) (1UL << (x))

#define TPM_PERIOD (48000)

// function prototypes
void Init_RGB_LEDs(void);
void Control_RGB_LEDs(uint8_t red_on, uint8_t green_on, uint8_t blue_on);

void Init_My_Red_LED(void);
void Init_My_Green_LED_PWM(uint16_t period);
void Init_My_Blue_LED(void);
void Control_My_LEDs(unsigned int red_on, unsigned int green_on, unsigned int blue_on);

#endif
// *******************************ARM University Program Copyright © ARM Ltd 2013*************************************   
