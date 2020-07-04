package io.picnicml.doddlemodel.preprocessing

import breeze.linalg.DenseVector
import breeze.stats.DescriptiveStats
import cats.syntax.option._
import io.picnicml.doddlemodel.CrossScalaCompat._
import io.picnicml.doddlemodel.data.Feature.FeatureIndex
import io.picnicml.doddlemodel.data.Features
import io.picnicml.doddlemodel.syntax.OptionSyntax._
import io.picnicml.doddlemodel.typeclasses.Transformer
import scala.Double.{MaxValue, MinValue}

case class QuantileDiscretizer(
  private val bucketCounts: DenseVector[Double],
  private val featureIndex: FeatureIndex,
  private val quantiles: Option[Seq[Seq[(Double, Double)]]] = None
) {
  private val numNumeric = featureIndex.numerical.columnIndices.length
  require(numNumeric == 0 || numNumeric == bucketCounts.length, "A quantile should be given for every numerical column")
}

/** An immutable preprocessor that discretizes numerical features into buckets based on quantiles.
  * Numerical feature values are converted to categorical features based on their distribution
  * */
object QuantileDiscretizer {

  /** Create a quantile discretizer which splits data into discrete evenly sized buckets.
    *
    * @param bucketCount The number of quantiles desired
    * @param featureIndex feature index associated with features - this is needed so that only numerical features are
    *                     transformed by this preprocessor; could be a subset of columns to be transformed
    *
    * @example Discretize a matrix into quartiles with two features: one numerical and one categorical.
    *   {{{
    *     import io.picnicml.doddlemodel.preprocessing.QuantileDiscretizer.ev
    *     import io.picnicml.doddlemodel.syntax.TransformerSyntax._
    *
    *     val featureIndex = FeatureIndex(List(NumericalFeature, CategoricalFeature))
    *     val x = DenseMatrix(
    *       List(-1.0, 0.0),
    *       List(0.0, 1.0),
    *       List(2.0, 0.0),
    *       List(5.0, 0.0)
    *     )
    *     // equivalently, DenseVector(4) could be used
    *     val bucketCounts = 4
    *     val discretizer = QuantileDiscretizer(bucketCounts, featureIndex).fit(x)
    *     val xTransformed = discretizer.transform(x)
    *   }}}
    */
  def apply(bucketCount: Int, featureIndex: FeatureIndex): QuantileDiscretizer = {
    val numNumeric = featureIndex.numerical.columnIndices.length
    val bucketCountExtended: DenseVector[Double] = DenseVector.fill(numNumeric) { bucketCount.toDouble }
    QuantileDiscretizer(bucketCountExtended, featureIndex)
  }

  implicit lazy val ev: Transformer[QuantileDiscretizer] = new Transformer[QuantileDiscretizer] {

    @inline override def isFitted(model: QuantileDiscretizer): Boolean = model.quantiles.isDefined

    override def fit(model: QuantileDiscretizer, x: Features): QuantileDiscretizer = {
      val discreteRangeArrays = model.featureIndex.numerical.columnIndices.zipWithIndex.map {
        case (colIndex, bucketCountsIndex) =>
          val colArray = x(::, colIndex).toScalaVector.sorted
          computeQuantiles(colArray, model.bucketCounts(bucketCountsIndex).toInt)
      }
      model.copy(quantiles = discreteRangeArrays.some)
    }

    override protected def transformSafe(model: QuantileDiscretizer, x: Features): Features = {
      val xCopy = x.copy
      model.featureIndex.numerical.columnIndices.zipWithIndex.foreach {
        case (colIndex, bucketsIndex) =>
          val buckets = model.quantiles.getOrBreak(bucketsIndex)
          (0 until xCopy.rows).foreach { rowIndex =>
            xCopy(rowIndex, colIndex) = buckets
              .indexWhere({
                case (lowerBound, upperBound) =>
                  lowerBound <= xCopy(rowIndex, colIndex) && xCopy(rowIndex, colIndex) <= upperBound
              })
              .toDouble
          }
      }
      xCopy
    }
  }

  private def computeQuantiles(target: Seq[Double], bucketCount: Int): Seq[(Double, Double)] = {
    val binPercentileWidth = 1.0 / bucketCount
    val targetArray = target.toArray
    // NOTE: Adds binPercentileWidth to make the range inclusive of 1
    val rangePairs =
      Range
        .BigDecimal(0, 1.0 + (binPercentileWidth / 2), binPercentileWidth)
        .map(_.toDouble)
        .map(DescriptiveStats.percentileInPlace(targetArray, _))
        .sliding(2)
        .map({case Seq(lowerBound, upperBound) => (lowerBound, upperBound)})
        .toSeq
    val headUpdate = rangePairs.headOption.getOrElse((MinValue, MaxValue)).copy(_1 = MinValue)
    val lastUpdate = rangePairs.lastOption.getOrElse((MinValue, MaxValue)).copy(_2 = MaxValue)
    rangePairs
      .updated(0, headUpdate)
      .updated(rangePairs.size - 1, lastUpdate)
  }
}
