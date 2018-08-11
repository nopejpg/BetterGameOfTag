//******************************************************************************
//  File:        util.c
//  Description: Util functions
//******************************************************************************

#include "utilities.h"
#include "crc.h"

/******************************************************************************\
* FUNCTION			Util_copyMemory
*	DESCRIPTION		Copies memory from src to dest
* PARAMETERS		pSrc - location of data to copy
*               pDest - destination of data
*               length - length of data
* RETURN				none
\******************************************************************************/
void Util_copyMemory(uint8_t *pSrc, uint8_t *pDest, uint32_t length)
{
  while (length--)
    *pDest++ = *pSrc++;
}

/******************************************************************************\
* FUNCTION			Util_fillMemory
*	DESCRIPTION		Fills memory with given value
* PARAMETERS		pDest - location of data to fill
*               length - length of data
*               value - data byte to fill with
* RETURN				none
\******************************************************************************/
void Util_fillMemory(uint8_t *pDest, uint32_t length, uint8_t value)
{
  while (length--)
    *pDest++ = value;
}

/******************************************************************************\
* FUNCTION			Util_computeChecksum
*	DESCRIPTION		Calculates the checksum of data
* PARAMETERS		pPtr - location of data
*               length - length of data
* RETURN				checksum
\******************************************************************************/
uint8_t Util_computeChecksum(const uint8_t *pData, uint16_t length, uint8_t initValue)
{
  uint8_t result = initValue;

  while(length--)
  {
    result += *pData++;
  }

  return result;
}    

/******************************************************************************\
* FUNCTION			Util_crc16
*	DESCRIPTION		Calculates the crc using the CCITT polynomial
* PARAMETERS		pData - location of data
*               length - length of data
* RETURN				crc
\******************************************************************************/
uint16_t Util_crc16(const void* pData, uint32_t length)
{
  uint16_t crc = CRC_fastCRC16((uint8_t*)pData, length, UTIL_CRC_INIT);
  
  return UTIL_CRC_FINALIZE(crc);
}

/*****************************************************************************\
* FUNCTION     Util_isSame
* DESCRIPTION  Checks to see if the two buffers are the same.
* PARAMETERS   pLeft -- the first set of bytes
*              pRight -- the second set of bytes
*              numBytes -- the number of bytes of each to compare
* RETURNS      true if they are the same
\*****************************************************************************/
bool Util_isSame(uint8_t *pLeft, uint8_t *pRight, uint32_t numBytes)
{
  for (; numBytes > 0; pLeft++, pRight++, numBytes--)
  {
    if (*pLeft != *pRight)
      return false;
  }

  return true;
}
