package com.viccodev.kotlindsp
import com.github.psambit9791.wavfile.WavFile
import org.jtransforms.fft.DoubleFFT_1D
import java.io.File

/*
 * Código desarrollado por:
 * Victor Manuel Pichardo Infante
 * Diana Vanessa Ortíz Martínez
 *
 * Este código se utiliza para la detección automática de tonos músicales.
 */

fun main()
{
    // Leer archivo WAV una sola vez
    val archivo = File("C:\\Users\\victo\\Downloads\\AndroidStudio\\app\\src\\main\\java\\com\\viccodev\\kotlindsp\\audios\\Tbn-ord-F3-mf-N-N.wav")
    val senial2 = WavFile.openWavFile(archivo)
    val sampleRate = senial2.sampleRate.toInt()
    val numChannels = senial2.numChannels // Número de canales en el archivo WAV

    // Leer toda la señal de audio una sola vez
    val longitudTotal = senial2.numFrames.toInt()
    val x = DoubleArray(longitudTotal)

    // Leer solo un canal (por ejemplo, el canal izquierdo si es estéreo)
    val buffer = DoubleArray(numChannels)
    for (j in x.indices)
    {
        senial2.readFrames(buffer, 1)
        x[j] = buffer[0] // Selecciona el primer canal (izquierdo)
    } // fin for

    // Seleccionar un segmento de un segundo
    val segmento = selectOneSecondSegment(x, sampleRate)

    val fftSize = sampleRate
    val fftSegment = DoubleArray(fftSize)
    System.arraycopy(segmento, 0, fftSegment, 0, fftSize)

    // Realizar FFT con JTransforms
    val fft = DoubleFFT_1D(fftSize.toLong())
    fft.realForward(fftSegment)

    // Obtener el tamaño del resultado de la FFT
    val fftResult = fftSegment // Este contiene el resultado de la FFT

    // Calcular el periodograma
    val resultArray = DoubleArray(fftSize / 2)
    for (k in resultArray.indices)
    {
        val realPart = fftResult[2 * k]
        val imaginaryPart = fftResult[2 * k + 1]
        resultArray[k] = (realPart * realPart + imaginaryPart * imaginaryPart) / fftSize
    } // fin 2do for

    // Encontrar picos significativos en la señal filtrada
    val threshold = 0.5
    val (marcas, indicePrincipal) = robustFindSignificativePeaks(resultArray, threshold)

    // Uso de la función findFundamentalFreq para calcular la frecuencia fundamental
    val fundamentalFrequency = findFundamentalFreq(marcas, sampleRate, fftSize)
    println("Frecuencia fundamental estimada: $fundamentalFrequency Hz")
}

// Este código utiliza y adapta el algoritmo HEPPIE desarrollado por Gerardo Abel Laguna-Sanchez.
// Repositorio original: https://github.com/galaguna/HEPPIE_pitch_estimation_algorithm
// Este código utiliza la biblioteca JDSP desarrollada por Sambit Paul.
// Citación recomendada: @software{sambit_paul_2024_10448582, doi: 10.5281/zenodo.10448582}
// Este código utiliza la biblioteca JTransforms para realizar la Transformada Rápida de Fourier (FFT).
// Repositorio de JTransforms: https://github.com/wendykierp/JTransforms







