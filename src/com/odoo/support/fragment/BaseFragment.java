/*
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * 
 */
package com.odoo.support.fragment;

import android.content.ContentResolver;
import android.content.SyncStatusObserver;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widgets.SwipeRefreshLayout;

import com.odoo.App;
import com.odoo.auth.OdooAccountManager;
import com.odoo.orm.OModel;
import com.odoo.support.AppScope;
import com.odoo.support.OUser;

/**
 * The Class BaseFragment.
 */
public abstract class BaseFragment extends Fragment implements OModuleHelper {

	/** The scope. */
	public AppScope scope;
	private OModel mDb = null;
	/** The list search adapter. */
	private ArrayAdapter<Object> listSearchAdapter;
	private Boolean showActionbar = true;
	private SyncStatusObserverListener mSyncStatusObserverListener = null;
	final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING
			| ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
	private Object mSyncObserverHandle;
	private OModel syncOberserverModel = null;
	private SearchView mSearchView = null;
	private OnSearchViewChangeListener mOnSearchViewChangeListener = null;
	private SwipeRefreshLayout mSwipeRefresh = null;

	/**
	 * Gets the query listener.
	 * 
	 * @param listAdapter
	 *            the list adapter
	 * @return the query listener
	 */
	public OnQueryTextListener getQueryListener(ArrayAdapter<Object> listAdapter) {
		listSearchAdapter = listAdapter;
		return queryListener;
	}

	/**
	 * Show action bar.
	 * 
	 * @param show
	 *            the show
	 */
	protected void showActionBar(Boolean show) {
		showActionbar = show;
	}

	/** The query listener. */
	final public OnQueryTextListener queryListener = new OnQueryTextListener() {

		private boolean isSearched = false;

		@Override
		public boolean onQueryTextChange(String newText) {

			if (TextUtils.isEmpty(newText)) {
				newText = "";
				if (isSearched && listSearchAdapter != null) {
					listSearchAdapter.getFilter().filter(null);
				}

			} else {
				isSearched = true;
				listSearchAdapter.getFilter().filter(newText);
			}

			return false;
		}

		@Override
		public boolean onQueryTextSubmit(String query) {
			return false;
		}
	};

	public void startFragment(Fragment fragment, Boolean addToBackState) {
		FragmentListener fragmentListener = (FragmentListener) getActivity();
		fragmentListener.startMainFragment(fragment, addToBackState);
	}

	public OModel db() {
		if (mDb == null)
			mDb = (OModel) databaseHelper(getActivity());
		return mDb;
	}

	/**
	 * Gets the resource string values
	 * 
	 * @param res_id
	 *            the res_id
	 * @return the string
	 */
	public String _s(int res_id) {
		return getActivity().getResources().getString(res_id);
	}

	/**
	 * gets the strings array from the resource value.
	 * 
	 * @param res_id
	 *            the res_id
	 * @return the string[]
	 */
	public String[] _sArray(int res_id) {
		return getActivity().getResources().getStringArray(res_id);
	}

	/**
	 * gets the drawable from resource values
	 * 
	 * @param res_id
	 *            the res_id
	 * @return the drawable
	 */
	public Drawable _d(int res_id) {
		return getActivity().getResources().getDrawable(res_id);
	}

	/**
	 * gets the integer value from resource values
	 * 
	 * @param res_id
	 *            the res_id
	 * @return the integer
	 */
	public Integer _i(int res_id) {
		return getActivity().getResources().getInteger(res_id);
	}

	/**
	 * gets the integer array from resource values.
	 * 
	 * @param res_id
	 *            the res_id
	 * @return the int[]
	 */
	public int[] _iArray(int res_id) {
		return getActivity().getResources().getIntArray(res_id);
	}

	/**
	 * gets the color from resource values
	 * 
	 * @param res_id
	 *            the res_id
	 * @return the int
	 */
	public int _c(int res_id) {
		return getActivity().getResources().getColor(res_id);
	}

	/**
	 * gets the dimension from resource values
	 * 
	 * @param res_id
	 *            the res_id
	 * @return the float
	 */
	public Float _dim(int res_id) {
		return getActivity().getResources().getDimension(res_id);
	}

	public App app() {
		return (App) getActivity().getApplicationContext();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!getActivity().getActionBar().isShowing() && showActionbar)
			getActivity().getActionBar().show();
		scope = new AppScope(getActivity());
		if (scope.main().getNavItem() != null)
			scope.main().setTitle(scope.main().getNavItem().getTitle());
		if (mSyncStatusObserverListener != null) {
			mSyncStatusObserver.onStatusChanged(0);
			mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask,
					mSyncStatusObserver);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mSyncObserverHandle != null) {
			ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
			mSyncObserverHandle = null;
		}
	}

	public void setHasSyncStatusObserver(
			SyncStatusObserverListener syncStatusObserver, OModel model) {
		mSyncStatusObserverListener = syncStatusObserver;
		syncOberserverModel = model;
	}

	private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
		/** Callback invoked with the sync adapter status changes. */
		@Override
		public void onStatusChanged(int which) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {

					boolean syncActive = ContentResolver.isSyncActive(
							OdooAccountManager.getAccount(getActivity(), OUser
									.current(getActivity()).getAndroidName()),
							syncOberserverModel.authority());
					boolean syncPending = ContentResolver.isSyncPending(
							OdooAccountManager.getAccount(getActivity(), OUser
									.current(getActivity()).getAndroidName()),
							syncOberserverModel.authority());
					mSyncStatusObserverListener.onStatusChange(syncActive
							| syncPending);
				}
			});
		}
	};

	public void setHasSearchView(OnSearchViewChangeListener listener,
			Menu menu, int menu_id) {
		mOnSearchViewChangeListener = listener;
		mSearchView = (SearchView) menu.findItem(menu_id).getActionView();
		mSearchView.setOnCloseListener(closeListener);
		mSearchView.setOnQueryTextListener(searchViewQueryListener);
		mSearchView.setIconifiedByDefault(true);
	}

	private SearchView.OnCloseListener closeListener = new SearchView.OnCloseListener() {

		@Override
		public boolean onClose() {
			// Restore the SearchView if a query was entered
			if (!TextUtils.isEmpty(mSearchView.getQuery())) {
				mSearchView.setQuery(null, true);
			}
			mOnSearchViewChangeListener.onSearchViewClose();
			return true;
		}
	};

	private OnQueryTextListener searchViewQueryListener = new OnQueryTextListener() {

		public boolean onQueryTextChange(String newText) {
			String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
			return mOnSearchViewChangeListener
					.onSearchViewTextChange(newFilter);
		}

		@Override
		public boolean onQueryTextSubmit(String query) {
			// Don't care about this.
			return true;
		}
	};

	public void setHasSwipeRefreshView(View parent, int resource_id,
			SwipeRefreshLayout.OnRefreshListener listener) {
		mSwipeRefresh = (SwipeRefreshLayout) parent.findViewById(resource_id);
		mSwipeRefresh.setOnRefreshListener(listener);
		mSwipeRefresh.setColorScheme(android.R.color.holo_blue_bright,
				android.R.color.holo_green_light,
				android.R.color.holo_orange_light,
				android.R.color.holo_red_light);
	}

	public void setSwipeRefreshing(Boolean refreshing) {
		if (mSwipeRefresh != null)
			mSwipeRefresh.setRefreshing(refreshing);
	}

	public void hideRefreshingProgress() {
		if (mSwipeRefresh != null) {
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					mSwipeRefresh.setRefreshing(false);
				}
			}, 1000);
		}
	}
}
