//******************************************************************************
//  File:        util.h
//  Description: Util functions
//******************************************************************************

#ifndef UTILITIES_H
#define UTILITIES_H

#include <stdint.h>
#include <stdbool.h>

#define ENABLE_INTERRUPTS() __bis_SR_register(GIE)
#define DISABLE_INTERRUPTS() __bic_SR_register(GIE)

#define UTIL_CRC_INIT           ((uint16_t) 0xFFFF)
#define UTIL_CRC_FINALIZE(CRC)  ((uint16_t) ~(CRC))

#define LENGTH_OF_ARRAY(a)  (sizeof(a) / sizeof(a[0]))

#define SWAP16(x)       (uint16_t)(((x << 8) | ((x >> 8) & 0xFF)))
#define SWAP32(x)       (SWAP16((uint16_t)(x>>16)) + (((uint32_t)SWAP16((uint16_t)x))<<16))

void Util_copyMemory(uint8_t *pSrc, uint8_t *pDest, uint32_t length);
void Util_fillMemory(uint8_t *pDest, uint32_t length, uint8_t value);
uint8_t Util_computeChecksum(const uint8_t *pData, uint16_t length, uint8_t initValue);
uint16_t Util_crc16(const void* pData, uint32_t length);
bool Util_isSame(uint8_t *pLeft, uint8_t *pRight, uint32_t numBytes);

#endif //UTILITIES_H
