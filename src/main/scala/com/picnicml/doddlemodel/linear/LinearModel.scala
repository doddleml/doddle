package com.picnicml.doddlemodel.linear

import breeze.linalg.{DenseMatrix, DenseVector}
import breeze.optimize._
import com.picnicml.doddlemodel.typeclasses.Predictor
import com.picnicml.doddlemodel.data.{Features, RealVector, Target}

trait LinearModel[A <: Predictor[A]] {
  this: Predictor[A] =>

  /** Parameters (weights) of a linear model, i.e. the state of the model. */
  protected val w: Option[RealVector]

  /** A function that creates a new linear model with parameters w. */
  protected def copy(w: RealVector): A

  /** A stateless function that predicts a target variable. */
  protected def predict(w: RealVector, x: Features): Target

  /** A stateless function that calculates the value of the loss function. */
  protected[linear] def loss(w: RealVector, x: Features, y: Target): Double

  /** A stateless function that calculates the gradient of the loss function wrt. model parameters. */
  protected[linear] def lossGrad(w: RealVector, x: Features, y: Target): RealVector

  override def isFitted: Boolean = this.w.isDefined

  override def predictSafe(x: Features): Target = this.predict(this.w.get, this.xWithBiasTerm(x))

  protected def maximumLikelihood(x: Features, y: Target, init: RealVector): RealVector = {
    val diffFunction = new DiffFunction[RealVector] {
      override def calculate(w: RealVector): (Double, RealVector) =
        (loss(w, x, y), lossGrad(w, x, y))
    }
    val lbfgs = new LBFGS[DenseVector[Double]](tolerance = 1e-4)
    lbfgs.minimize(diffFunction, init)
  }

  protected def xWithBiasTerm(x: Features): Features =
    DenseMatrix.horzcat(DenseMatrix.ones[Double](x.rows, 1), x)
}
