# 🧠 Offline STT Technologies Comparison on Android

## 📋 Overview

This project is part of a master's thesis focused on the **comparative analysis of selected offline Speech-to-Text (STT) technologies** for Android. The goal is to evaluate and compare **effectiveness**, **performance**, and **resource usage** of the selected STT engines.

Only **free, open-source** solutions are considered in this study:
- **Sherpa-ONNX**
- **Sherpa-NCNN**
- **Vosk**

Each of these tools uses different strategies for implementing and optimizing STT models, offering a broad perspective on available offline solutions for mobile devices.

## 🎯 Objective

To determine which STT engine provides the best balance between:
- **Recognition accuracy**
- **Runtime performance**
- **Resource efficiency** (CPU, RAM, storage, battery)

## 🧪 Tested Technologies

| Engine         | Framework      | Model Format | Key Characteristics                          |
|----------------|----------------|--------------|-----------------------------------------------|
| Sherpa-ONNX    | ONNX Runtime   | ONNX         | High accuracy, flexible, optimized for CPU/GPU|
| Sherpa-NCNN    | ncnn           | Param/bin    | Lightweight, mobile-optimized inference       |
| Vosk           | Kaldi-based    | .pb / .zip   | Reliable, widely used, moderate requirements  |
