package com.servabosafe.shadow.adapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class EasyAdapter< T > extends BaseAdapter
{
  protected WeakReference< Context > mContext = null;
  protected ArrayList< T >           mItems;

  // private constructor
  private EasyAdapter()
  {
  }

  public EasyAdapter( Context context )
  {
    // call so eclipse doesn't complain about the private constructor
    this();

    // hook up the Context reference
    mContext = new WeakReference< Context >( context );

    // load up our backing list
    mItems = new ArrayList< T >();
  }

  public EasyAdapter( Context context, ArrayList< T > items )
  {
    // call so eclipse doesn't complain about the private constructor
    this();

    // hook up the Context reference
    mContext = new WeakReference< Context >( context );

    // popuplate our backing list
    mItems = items;
  }

  @Override
  public int getCount()
  {
    return mItems.size();
  }

  @Override
  public T getItem( int position )
  {
    return mItems.get( position );
  }

  @Override
  public long getItemId( int position )
  {
    return mItems.get( position ).hashCode();
  }

  @Override
  public abstract View getView( int position, View convertView, ViewGroup parent );

  public void add( T newItem )
  {
    mItems.add( newItem );
  }

  public void clear()
  {
    mItems = null;
    mItems = new ArrayList< T >();

    notifyDataSetChanged();
  }

}
