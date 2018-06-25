#ifndef DMA_H
#define DMA_H
#include <MKL25Z4.h>
void Init_DMA(void);
void Init_DMA_For_Playback(void);
void DMA0_IRQHandler(void);
void Play_Recording(const char * source, uint32_t count);



#endif
