import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/**
  * Main
  */

object Main{

  import java.util.concurrent.ArrayBlockingQueue
  import java.util.concurrent.{Executors,ExecutorService}

  def main(args: Array[String]): Unit = {
    /*
     * transform will read the input file contained in input/ directory,
     * normalize them and then write in the output/ directory.
     */
    val inputFiles = new java.io.File("./input").listFiles.filter(_.getName.endsWith(".json"))
    val numberOfFiles = inputFiles.size
    val q = new ArrayBlockingQueue[String](numberOfFiles, true)
    for (f <- inputFiles) {q.put(f.getName)}

    val threadNumbers = Runtime.getRuntime.availableProcessors
    val pool: ExecutorService = Executors.newFixedThreadPool(threadNumbers)
    val doneSignal: CountDownLatch = new CountDownLatch(threadNumbers)

    println(threadNumbers + " " + "threads will be used.")

      try {
      1 to threadNumbers foreach { x =>
        pool.execute(
          new Runnable {
            def run {
              try {
                val tn = new TweetNormalizer
                while (q.size() > 0) {
                  val fileName = q.take()
                  println("Processing file " + fileName)
                  println(pool + "threads will be used.")
                  tn.transform(fileName)
                }
              }finally {
                doneSignal.countDown()
              }
            }
          }
        )
      }
    } finally {
      pool.shutdown()
    }

    doneSignal.await()
    val ts = new TweetStatistics
    ts.setStats()
    ts.showRatios()
  }


    /*
     * run Spark: set data frame and make queries (SQL like)
     */
    /*
    val sa = new SparkAnalysis
    sa.run()
    sa.stop()
    */
}