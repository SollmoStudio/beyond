package beyond.metrics

trait NumberOfRequestsPerSecondMBean {
  def getNumberOfRequestsPerSecond: Int
  def increase(): Unit
}
