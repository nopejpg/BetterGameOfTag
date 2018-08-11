//******************************************************************************
//  File:        crc.h
//  Description: CRC functions using CCITT algorithm
//******************************************************************************

#ifndef CRC_H
#define CRC_H

#include <stdint.h>

uint16_t CRC_fastCRC16(uint8_t *pData, uint32_t length, uint16_t crc);
uint16_t CRC_slowCRC16(uint8_t *pData, uint32_t length, uint16_t crc);

#endif //CRC_H
