package nn4j.expr;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.random.impl.BernoulliDistribution;
import org.nd4j.linalg.factory.Nd4j;

import nn4j.utils.NDArrayCache;

public class Dropout extends Expr{

	private Expr input;
	private boolean training;
	private float rejectProb;

	public Dropout(Expr input,float acceptProb,boolean training){
		super(input);
		this.input=input;
		this.rejectProb=1.0f-acceptProb;
		this.training=training;
	}
	
	private INDArray acceptArray;

	@Override
	public INDArray doForward() {
		INDArray preout=input.forward();
		acceptArray=NDArrayCache.get(preout.shape());
		if(training)
		{
			Nd4j.getExecutioner().exec(new BernoulliDistribution(acceptArray, rejectProb));
		}else{
			acceptArray.assign(rejectProb);
		}
		output=preout.mul(acceptArray);
		return output;
	}

	@Override
	public void doBackward(INDArray epsilon) {
		input.backward(epsilon.muli(acceptArray));
	}

	@Override
	public void clear() {
		if(output!=null)
		{
			NDArrayCache.store(output);
			output=null;
			NDArrayCache.store(acceptArray);
			acceptArray=null;
			for(Expr e : inputs){
				e.clear();
			}
		}
	}

	@Override
	public int[] shape() {
		return input.shape();
	}
	

}
