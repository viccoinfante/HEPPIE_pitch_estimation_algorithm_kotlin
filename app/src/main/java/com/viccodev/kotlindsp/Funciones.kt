package com.viccodev.kotlindsp

import kotlin.math.sin

/*
 * Código desarrollado por:
 * Victor Manuel Pichardo Infante
 * Diana Vanessa Ortíz Martínez
 *
 * Este código se utiliza para la detección automática de tonos músicales.
 */

// Definición de la función FIR Filter
fun firFilter(h: DoubleArray, x: DoubleArray): DoubleArray
{
    val lenH = h.size
    val lenX = x.size
    val y = DoubleArray(lenX)

    for (n in 0 until lenX)
    {
        var sum = 0.0
        for (k in 0 until lenH)
        {
            if (n - k >= 0)
            {
                sum += h[k] * x[n - k]
            } // fin if
        } // fin 2do for
        y[n] = sum
    } // fin 1er for

    return y
} //fin firFilter

//Aplicación del filtro pasa bajas
fun applyLowPassFilter(h: DoubleArray, w: DoubleArray, ): DoubleArray
{
    // Aplicar el filtro FIR usando convolución
    val p = firFilter(h, w)

    // Compensar el retraso del filtro
    val hLen = h.size
    val hDelay = hLen / 2
    val pLs = p.copyOfRange(hDelay, p.size)

    return pLs
}//fin applyLowPassFilter

fun convolve(x: DoubleArray, h: DoubleArray): DoubleArray
{
    val len = x.size + h.size - 1
    val result = DoubleArray(len)
    for (i in x.indices) {
        for (j in h.indices) {
            result[i + j] += x[i] * h[j]
        }
    }
    return result

}//fin convolve

// Encontrar el índice del valor máximo
fun findMaxIndex(array: DoubleArray): Int
{
    var maxIndex = 0
    var maxValue = array[0]
    for (i in array.indices)
    {
        if (array[i] > maxValue)
        {
            maxValue = array[i]
            maxIndex = i
        } // fin if
    } //fin for
    return maxIndex
} // fin findMaxIndex

fun cleanAroundIx(x: DoubleArray, ix: Int, radio: Int): DoubleArray
{
    val xLen = x.size
    // Copiar el vector de entrada
    val y = x.copyOf()

    for (i in -radio..radio)
    {
        val currentIx = ix - i
        if (currentIx in 0 until xLen)
        {
            // Establecer en cero los valores alrededor del índice
            y[currentIx] = 0.0
        } //fin if
    } //fin for

    return y
} // fin cleanAroundIx


fun estimatePositivePeakWidth(y: DoubleArray, targetIx: Int, floorLevelThreshold: Double): Int {
    val len = y.size
    val floorLevel = floorLevelThreshold * y[targetIx]

    var upIx = len - 1
    var downIx = 0
    // Ascendente
    var i = targetIx + 1
    while (i < len && y[i] > floorLevel)
    {
        i++
    } // fin 1er while
    if (i < len && y[i] <= floorLevel)
    {
        upIx = i
    } // fin 1er if

    // Descendente
    i = targetIx - 1
    while (i >= 0 && y[i] > floorLevel)
    {
        i--
    } //fin 2do while
    if (i >= 0 && y[i] <= floorLevel) {
        downIx = i
    } //fin 1er if

    return upIx - downIx
} // fin estimatePositivePeakWidth

fun selectOneSecondSegment(x: DoubleArray, fs: Int): DoubleArray
{
    val len = x.size
    // Selección de un segmento de un segundo de duración
    val segmentSize = fs
    val s: DoubleArray

    if (len <= segmentSize)
    {
        // Si la longitud de la señal es menor o igual a la frecuencia de muestreo, tomar toda la señal
        s = x.copyOf()
    } //fin 1er if
    else
    {
        // Intento en el valor máximo de la señal
        val maxIndex = x.indices.maxByOrNull { x[it] } ?: 0
        var ini = maxIndex
        var fin = ini + segmentSize

        if (fin <= len)
        {
            // Si se puede seleccionar un segmento de un segundo desde el máximo
            s = x.copyOfRange(ini, fin)
        } // fin 2do if
        else
        {
            // Intento desde la mitad de la señal
            ini = len / 2 - segmentSize / 2
            fin = ini + segmentSize

            if (fin <= len)
            {
                s = x.copyOfRange(ini, fin)
            } // fin 3er if
            else
            {
                // Si no es posible, tomar las últimas muestras
                ini = len - segmentSize
                s = x.copyOfRange(ini, len)
            } // fin 3er else
        } // fin 2do else
    } // fin 1er else

    return s
} //fin selectOneSecondSegment

fun findFundamentalFreq(m: IntArray, fs: Int, fftSize: Int): Double
{
    val fo = fs.toDouble() / fftSize
    val len = m.size

    val distances = mutableListOf<Int>()
    distances.add(0)

    for (i in 0 until len - 1)
    {
        if (m[i] == 1) {
            val nextMaxIx = findNextLocalMax(m, i)
            if (nextMaxIx > 0)
            {
                val distance = nextMaxIx - i
                if (distance > 0)
                {
                    distances.add(distance)

                } // fin 3er if
            } // fin 2do if
        } // fin 1er if
    } // fin for



    val minDistance = distances.filter { it > 0 }.minOrNull() ?: 0
    if (minDistance > 0)
    {
        val fundFreq = minDistance * fo
        return fundFreq
    } // fin 4to if
    else
    {
        return 0.0
    } // fin else
} // fin findFundamentalFreq

fun findNextLocalMax(m: IntArray, targetIx: Int): Int {
    val len = m.size
    var nextMaxIx = 0

    // Buscar hacia adelante
    var i = targetIx + 1
    while (i < len && m[i] != 1) {
        i++
    }

    if (i < len && m[i] == 1) {
        nextMaxIx = i
    }

    return nextMaxIx
}

fun robustFindSignificativePeaks(y: DoubleArray, threshold: Double): Pair<IntArray, Int>
{
    val len = y.size
    val m = IntArray(len) { 0 }

    // Filtro diferenciador
    val b_d = doubleArrayOf(1.0, -1.0)
    val d = convolve(y, b_d)
    val d_ls = d.sliceArray(1 until d.size)

    // Estimación de wavelet_len
    val peakIx = findMaxIndex(d_ls)
    val p_peak_width = estimatePositivePeakWidth(d_ls, peakIx, 0.01)
    val waveletLen = 2 * p_peak_width

    // Perfil de Wavelet
    val t = DoubleArray(waveletLen) { i -> 2 * Math.PI / waveletLen * i }
    val b_r = DoubleArray(t.size) { i -> -sin(t[i]) }

    // Función de correlación
    val r = convolve(d_ls, b_r)
    val r_ls = r.sliceArray(p_peak_width until r.size)

    // Marcado por defecto en la frecuencia cero
    m[0] = 1
    val mainIx = r_ls.indices.maxByOrNull { r_ls[it] } ?: 0
    val significativeLevel = r_ls[mainIx] * threshold
    m[mainIx] = 1

    val cleanBinWidth = (waveletLen * 2.5).toInt()
    var r_w = cleanAroundIx(r_ls, mainIx, cleanBinWidth)

    var maxIx = r_w.indices.maxByOrNull { r_w[it] } ?: 0
    var a = r_w[maxIx]
    while (a > significativeLevel) {
        m[maxIx] = 1
        r_w = cleanAroundIx(r_w, maxIx, cleanBinWidth)
        maxIx = r_w.indices.maxByOrNull { r_w[it] } ?: 0
        a = r_w[maxIx]
    }

    return Pair(m, mainIx)
}

// Este código utiliza y adapta el algoritmo HEPPIE desarrollado por Gerardo Abel Laguna-Sanchez.
// Repositorio original: https://github.com/galaguna/HEPPIE_pitch_estimation_algorithm
