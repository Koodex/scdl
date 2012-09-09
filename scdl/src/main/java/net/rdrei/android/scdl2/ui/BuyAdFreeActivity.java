package net.rdrei.android.scdl2.ui;

import net.rdrei.android.mediator.MessageMediator;
import net.rdrei.android.scdl2.ApplicationPreferences;
import net.rdrei.android.scdl2.R;
import net.rdrei.android.scdl2.ui.BuyAdFreeTeaserFragment.BuyAdFreeFragmentContract;
import net.robotmedia.billing.BillingRequest.ResponseCode;
import roboguice.activity.RoboFragmentActivity;
import roboguice.util.Ln;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.google.inject.Inject;

/**
 * This class acts as net.rdrei.android.mediator between the three different
 * fragments loaded into it that are responsible for a) the billing logic b) the
 * teaser view leading to a purchase, giving explanations c) the thanks view
 * after a successful purchase
 * 
 * @author pascal
 */
public class BuyAdFreeActivity extends RoboFragmentActivity implements
		BuyAdFreeFragmentContract {

	private static final String BILLING_FRAGMENT_TAG = "BILLING";

	@Inject
	private ApplicationPreferences mPreferences;

	@Inject
	private ActionBar mActionBar;

	@Inject
	private MessageMediator.Receiver mTeaserReceiver;

	private Fragment mContentFragment;
	private AdFreeBillingFragment mBillingFragment;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.buy_ad_free);
		mActionBar.setDisplayHomeAsUpEnabled(true);

		loadFragments();
	}

	private void loadFragments() {
		final FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();

		if (mPreferences.isAdFree()) {
			mContentFragment = BuyAdFreeThanksFragment.newInstance();
		} else {
			mBillingFragment = AdFreeBillingFragment.newInstance();
			mContentFragment = BuyAdFreeTeaserFragment
					.newInstance(mTeaserReceiver);

			transaction.add(mBillingFragment, BILLING_FRAGMENT_TAG);
		}

		transaction.add(R.id.main_layout, mContentFragment).commit();
	}

	@Override
	public void onBuyClicked() {
		final BuyAdFreeTeaserFragment fragment = (BuyAdFreeTeaserFragment) mContentFragment;
		fragment.setBillingEnabled(false);
		mBillingFragment.requestPurchase();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			final Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

			startActivity(intent);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBillingChecked(final boolean supported) {
		final Message message = Message.obtain();

		if (supported) {
			message.what = BuyAdFreeTeaserFragment.MSG_BILLING_SUPPORTED;
		} else {
			message.what = BuyAdFreeTeaserFragment.MSG_BILLING_UNSUPPORTED;
		}

		mTeaserReceiver.dispatch(message);
	}

	@Override
	public void onBuyError(final ResponseCode response) {
		Ln.e("IAB error: %s", response.toString());

		mTeaserReceiver.dispatch(Message.obtain(null,
				BuyAdFreeTeaserFragment.MSG_PURCHASE_ERROR));
	}

	@Override
	public void onBuySuccess() {
		mPreferences.setAdFree(true);

		getSupportFragmentManager().beginTransaction().remove(mContentFragment)
				.add(R.id.main_layout, BuyAdFreeThanksFragment.newInstance())
				.commit();
	}

	@Override
	public void onBuyRevert() {
		mPreferences.setAdFree(false);

		mTeaserReceiver.dispatch(Message.obtain(null,
				BuyAdFreeTeaserFragment.MSG_PURCHASE_REVERTED));
	}

	@Override
	public void onBuyCancel() {
		mTeaserReceiver.dispatch(Message.obtain(null,
				BuyAdFreeTeaserFragment.MSG_PURCHASE_CANCELLED));
	}

	@Override
	public void onPurchaseRequested() {
		mTeaserReceiver.dispatch(Message.obtain(null,
				BuyAdFreeTeaserFragment.MSG_PURCHASE_REQUESTED));
	}
}
