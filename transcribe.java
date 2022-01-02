


/**
 * Performs non-blocking speech recognition on remote FLAC file and prints the transcription.
 *
 * @param gcsUri the path to the remote LINEAR16 audio file to transcribe.
 */

 // we can can call this method in a for loop and use it on all the audio files in the bucket
public static void asyncRecognizeGcs(String gcsUri) throws Exception {
    // Configure polling algorithm
    SpeechSettings.Builder speechSettings = SpeechSettings.newBuilder();
    TimedRetryAlgorithm timedRetryAlgorithm =
        OperationTimedPollAlgorithm.create(
            RetrySettings.newBuilder()
                .setInitialRetryDelay(Duration.ofMillis(500L))
                .setRetryDelayMultiplier(1.5)
                .setMaxRetryDelay(Duration.ofMillis(5000L))
                .setInitialRpcTimeout(Duration.ZERO) // ignored
                .setRpcTimeoutMultiplier(1.0) // ignored
                .setMaxRpcTimeout(Duration.ZERO) // ignored
                .setTotalTimeout(Duration.ofHours(24L)) // set polling timeout to 24 hours
                .build());
    speechSettings.longRunningRecognizeOperationSettings().setPollingAlgorithm(timedRetryAlgorithm);
  
    // Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
    try (SpeechClient speech = SpeechClient.create(speechSettings.build())) {
  
      // Configure remote file request for FLAC
      RecognitionConfig config =
          RecognitionConfig.newBuilder()
              .setEncoding(AudioEncoding.FLAC)
              .setLanguageCode("en-US")
              .setSampleRateHertz(16000)
              .build();
      RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();
  
      // Use non-blocking call for getting file transcription
      OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response =
          speech.longRunningRecognizeAsync(config, audio);
      while (!response.isDone()) {
        System.out.println("Waiting for response...");
        Thread.sleep(10000);
      }
  
      List<SpeechRecognitionResult> results = response.get().getResultsList();
  
      for (SpeechRecognitionResult result : results) {
        // There can be several alternative transcripts for a given chunk of speech. Just use the
        // first (most likely) one here.
        SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
        System.out.printf("Transcription: %s\n", alternative.getTranscript());
      }
    }
  }