package com.stone.richeditor;

import java.util.ArrayList;
import java.util.List;

import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import static android.util.Log.*;

/**
 * 这是一个富文本编辑器，给外部提供insertImage接口，添加的图片跟当前光标所在位置有关
 * 
 * @author xmuSistone
 * 
 */
@SuppressLint({ "NewApi", "InflateParams" })
public class RichTextEditor extends ScrollView {
	private static final int EDIT_PADDING = 10; // edittext常规padding是10dp
	private static final int EDIT_FIRST_PADDING_TOP = 10; // 第一个EditText的paddingTop值
    private static final int IMG_SRC_START = 5;
	private static final int IMG_SRC_END = 6;

	private int viewTagIndex = 1; // 新生的view都会打一个tag，对每个view来说，这个tag是唯一的。
	private LinearLayout allLayout; // 这个是所有子view的容器，scrollView内部的唯一一个ViewGroup
	private LayoutInflater inflater;
	private OnKeyListener keyListener; // 所有EditText的软键盘监听器
	private OnClickListener btnListener; // 图片右上角红叉按钮监听器
	private OnFocusChangeListener focusListener; // 所有EditText的焦点监听listener
	private EditText lastFocusEdit; // 最近被聚焦的EditText
	private EditText firstEdit;
	private LayoutTransition mTransitioner; // 只在图片View添加或remove时，触发transition动画
	private int editNormalPadding = 0; //
	private int disappearingImageIndex = 0;

	public RichTextEditor(Context context) {
		this(context, null);
	}

	public RichTextEditor(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public RichTextEditor(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		inflater = LayoutInflater.from(context);

		// 1. 初始化allLayout
		allLayout = new LinearLayout(context);
		allLayout.setOrientation(LinearLayout.VERTICAL);
		allLayout.setBackgroundColor(Color.WHITE);
		setupLayoutTransitions();
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		addView(allLayout, layoutParams);

		// 2. 初始化键盘退格监听
		// 主要用来处理点击回删按钮时，view的一些列合并操作
		keyListener = new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN
						&& event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
					EditText edit = (EditText) v;
					onBackspacePress(edit);
				}
				return false;
			}
		};

		// 3. 图片叉掉处理
		btnListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				RelativeLayout parentView = (RelativeLayout) v.getParent();
				onImageCloseClick(parentView);
			}
		};

		focusListener = new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					lastFocusEdit = (EditText) v;
				}
			}
		};

		LinearLayout.LayoutParams firstEditParam = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		editNormalPadding = dip2px(EDIT_PADDING);
		 firstEdit = createEditText("input here",
				dip2px(EDIT_FIRST_PADDING_TOP));
		allLayout.addView(firstEdit, firstEditParam);
		lastFocusEdit = firstEdit;
	}

	/**
	 * 处理软键盘backSpace回退事件
	 * 
	 * @param editTxt
	 *            光标所在的文本输入框
	 */
	private void onBackspacePress(EditText editTxt) {
		int startSelection = editTxt.getSelectionStart();
		// 只有在光标已经顶到文本输入框的最前方，在判定是否删除之前的图片，或两个View合并
		if (startSelection == 0) {
			int editIndex = allLayout.indexOfChild(editTxt);
			View preView = allLayout.getChildAt(editIndex - 1); // 如果editIndex-1<0,
																// 则返回的是null
			if (null != preView) {
				if (preView instanceof RelativeLayout) {
					// 光标EditText的上一个view对应的是图片
					onImageCloseClick(preView);
				} else if (preView instanceof EditText) {
					// 光标EditText的上一个view对应的还是文本框EditText
					String str1 = editTxt.getText().toString();
					EditText preEdit = (EditText) preView;
					String str2 = preEdit.getText().toString();

					// 合并文本view时，不需要transition动画
					allLayout.setLayoutTransition(null);
					allLayout.removeView(editTxt);
					allLayout.setLayoutTransition(mTransitioner); // 恢复transition动画

					// 文本合并
					preEdit.setText(str2 + str1);
					preEdit.requestFocus();
					preEdit.setSelection(str2.length(), str2.length());
					lastFocusEdit = preEdit;
				}
			}
		}
	}

	/**
	 * 处理图片叉掉的点击事件
	 * 
	 * @param view
	 *            整个image对应的relativeLayout view
	 * @type 删除类型 0代表backspace删除 1代表按红叉按钮删除
	 */
	private void onImageCloseClick(View view) {
		if (!mTransitioner.isRunning()) {
			disappearingImageIndex = allLayout.indexOfChild(view);
			allLayout.removeView(view);
		}
	}

	/**
	 * 生成文本输入框
	 */
	private EditText createEditText(String hint, int paddingTop) {
		EditText editText = (EditText) inflater.inflate(R.layout.edit_item1,
				null);
		editText.setOnKeyListener(keyListener);
		editText.setTag(viewTagIndex++);
		editText.setPadding(editNormalPadding, paddingTop, editNormalPadding, 0);
		editText.setHint(hint);
		editText.setOnFocusChangeListener(focusListener);
		return editText;
	}

	/**
	 * 生成图片View
	 */
	private RelativeLayout createImageLayout() {
		RelativeLayout layout = (RelativeLayout) inflater.inflate(
				R.layout.edit_imageview, null);
		layout.setTag(viewTagIndex++);
		View image = layout.findViewById(R.id.edit_imageView);
		final View closeView = layout.findViewById(R.id.image_close);
		closeView.setTag(layout.getTag());
		closeView.setOnClickListener(btnListener);
		image.setOnClickListener(new View.OnClickListener() {
									 @Override
									 public void onClick(View v) {
                                            closeView.setVisibility(View.VISIBLE);
									 }
								 });

		return layout;
	}

	/**
	 * 根据绝对路径添加view
	 * 
	 * @param imagePath
	 */
	public void insertImage(String imagePath) {
		Log.d("Danny", "imagePath = "+imagePath);
		Bitmap bmp = getScaledBitmap(imagePath, 480);
		if (bmp == null) {
            Log.d("Danny", "bmp is null....");
		}
		insertImage(bmp, imagePath);
	}


	/**
	 * 插入一张图片
	 */
	private void insertImage(Bitmap bitmap, String imagePath) {
		String lastEditStr = lastFocusEdit.getText().toString();
		int cursorIndex = lastFocusEdit.getSelectionStart();
		String editStr1 = lastEditStr.substring(0, cursorIndex).trim();
		int lastEditIndex = allLayout.indexOfChild(lastFocusEdit);
        Log.d("Danny", "inertImage,....lastEditIndex = "+lastEditIndex);
		if (lastEditStr.length() == 0 || editStr1.length() == 0) {
			// 如果EditText为空，或者光标已经顶在了editText的最前面，则直接插入图片，并且EditText下移即可
			Log.d("Danny", "inertImage,00000");
			if (bitmap == null)
				Log.d("Danny", "bitmap is null....");
			addImageViewAtIndex(lastEditIndex, bitmap, imagePath);
		} else {
			// 如果EditText非空且光标不在最顶端，则需要添加新的imageView和EditText
			Log.d("Danny", "inertImage,!!!!!!!!100000");
			lastFocusEdit.setText(editStr1);
			String editStr2 = lastEditStr.substring(cursorIndex).trim();
			if (allLayout.getChildCount() - 1 == lastEditIndex
					|| editStr2.length() > 0) {
				Log.d("Danny", "inertImage,childCount");
				addEditTextAtIndex(lastEditIndex + 1, editStr2);
			}

			addImageViewAtIndex(lastEditIndex + 1, bitmap, imagePath);
			lastFocusEdit.requestFocus();
			lastFocusEdit.setSelection(editStr1.length(), editStr1.length());
		}
		hideKeyBoard();
	}

	/**
	 * 隐藏小键盘
	 */
	public void hideKeyBoard() {
		InputMethodManager imm = (InputMethodManager) getContext()
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(lastFocusEdit.getWindowToken(), 0);
	}
/* add by danny*/
	public void setContent(String content){
		if (content != null && content.contains("<img>")) {
			Log.d("Danny", "setContent: content contains img.");
			//这点欠妥当，后续修改,目前只是考虑理想状态的，后续许多特殊情况需要考虑。
			int startIndex = content.indexOf("<img>", -1);
			int endIndex = content.indexOf("</img>", -1);
			String imgStr = content.substring(0, endIndex+IMG_SRC_END);
			String secretStr = content.substring(endIndex+IMG_SRC_END, content.length());
			String imgPath = imgStr.substring(startIndex+IMG_SRC_START, endIndex);
            if (startIndex != 0 && startIndex != -1) {
				String sub = imgStr.substring(0, startIndex);
				Log.d("Danny", "allLayout.getChildCount() === "+allLayout.getChildCount()+": firstEdit = "+firstEdit);
				if (allLayout.getChildCount() == 1 && firstEdit != null) {
					firstEdit.setText(sub);
				} else if (allLayout.getChildCount() != 1){
					Log.d("Danny", "allLayout.getChildCount() === "+allLayout.getChildCount());
					if (allLayout.getChildAt(allLayout.getChildCount()-1) instanceof EditText){
					//if (lastFocusEdit != null){
						Log.d("Danny", "..................jiojutijhtjhij");
						EditText tmpEt = (EditText)allLayout.getChildAt(allLayout.getChildCount()-1);
						String tmpEtStr = tmpEt.getText().toString();
						Log.d("Danny", "..................jiojutijhtjhij et===="+tmpEt+": stri==="+tmpEtStr);
						if (tmpEtStr != null && tmpEtStr.length() != 0) {
							Log.d("Danny", "ettttttttttttttttttttttttttttttttttttttttttt");
							tmpEt.setText(tmpEtStr+"\n"+sub);
						} else if (tmpEtStr.length() ==0 || tmpEtStr ==null){
							Log.d("Danny", "rirririririririririririisub === "+sub);
							tmpEt.setText(sub);
							Log.d("Danny", "TMPeT = ="+tmpEt.getText());
						}
					} else {
						addEditTextAtIndex( allLayout.getChildCount(),"");
						((EditText)allLayout.getChildAt(allLayout.getChildCount()-1)).setText(sub);
					}
				}
				//insertImage(imgPath, null);
			} /*else if (startIndex == 0){
				if (imgPath != null)
				insertImage(imgPath, null);
			}*/


			/*String subString = content.substring(index+5, content.length());
			int indexEnd = subString.indexOf(">", 0);
			String src = subString.substring(0, indexEnd);

			Log.d("Danny", "setContent: the index = "+index+": end = "+indexEnd+": src = "+src);*/
			lastFocusEdit = (EditText)allLayout.getChildAt(allLayout.getChildCount()-1);
			lastFocusEdit.requestFocus();
			lastFocusEdit.setSelection(lastFocusEdit.getText().length(), lastFocusEdit.getText().length());
            if (imgPath != null)
			insertImage(imgPath, null);
			Log.d("Danny", "inertImage ==== count = "+allLayout.getChildCount());
			if (!(allLayout.getChildAt(allLayout.getChildCount()-1) instanceof EditText)) {
				addEditTextAtIndex(allLayout.getChildCount(), "");
			}
			Log.d("Danny", "secretStr = ="+secretStr+": count = "+allLayout.getChildCount()+"::: ");
			lastFocusEdit = (EditText)allLayout.getChildAt(allLayout.getChildCount()-1);
			lastFocusEdit.requestFocus();
			lastFocusEdit.setSelection(lastFocusEdit.getText().length(), lastFocusEdit.getText().length());
            Log.d("Danny", "the last focusedit====="+lastFocusEdit);
			if (secretStr != null && secretStr.length() != 0)
			    setContent(secretStr);
		} else if (content != null){
			if (allLayout.getChildCount() != 1 && lastFocusEdit != null){
				Log.d("Danny", "content..........."+lastFocusEdit+ ": Content = ="+content);
				lastFocusEdit.setText(content);
			}else if (allLayout.getChildCount() == 1) {
				lastFocusEdit.setText(content);
			}
		}
		lastFocusEdit = (EditText)allLayout.getChildAt(allLayout.getChildCount()-1);
		lastFocusEdit.requestFocus();
		lastFocusEdit.setSelection(lastFocusEdit.getText().length(), lastFocusEdit.getText().length());
	}
	public void insertImage(String path, String des){
		Bitmap bmp = getScaledBitmap(path, 480);
		if (bmp == null) {
			Log.d("Danny", "insertImage new bmp is null....");
		}
		String lastEditStr = lastFocusEdit.getText().toString();
		int cursorIndex = lastFocusEdit.getSelectionStart();
		String editStr1 = lastEditStr.substring(0, cursorIndex).trim();
		int lastEditIndex = allLayout.indexOfChild(lastFocusEdit);
		Log.d("Danny", "inertImage,....lastEditIndex = "+lastEditIndex);
		if (lastEditStr.length() == 0 ) {
			// 如果EditText为空，或者光标已经顶在了editText的最前面，则直接插入图片，并且EditText下移即可
			Log.d("Danny", "inertImage,00000");
			addImageViewAtIndex(lastEditIndex, bmp, path);
		} else {
			addImageViewAtIndex(lastEditIndex + 1,  path, bmp);
		}
		Log.d("Danny", "inertImage++++childCount = "+allLayout.getChildCount());
	//	}
		hideKeyBoard();
	}
	/**
	 * 在特定位置插入EditText
	 * 
	 * @param index
	 *            位置
	 * @param editStr
	 *            EditText显示的文字
	 */
	private void addEditTextAtIndex(final int index, String editStr) {
		EditText editText2 = createEditText("nihao", getResources()
				.getDimensionPixelSize(R.dimen.edit_padding_top));
		editText2.setText(editStr);

		// 请注意此处，EditText添加、或删除不触动Transition动画
		allLayout.setLayoutTransition(null);
		allLayout.addView(editText2, index);
		allLayout.setLayoutTransition(mTransitioner); // remove之后恢复transition动画
	}

	/**
	 * 在特定位置添加ImageView:: by danny
	 */
	private void addImageViewAtIndex(final int index,
									 String imagePath,  Bitmap bmp) {
		if (bmp == null){
			Log.d("Danny", "addImageViewAtIndex...");
		}
		final RelativeLayout imageLayout = createImageLayout();
		DataImageView imageView = (DataImageView) imageLayout
				.findViewById(R.id.edit_imageView);
		imageView.setImageBitmap(bmp);
		imageView.setBitmap(bmp);
		imageView.setAbsolutePath(imagePath);
		Log.d("Danny", "bmp.getHeight() = "+bmp.getHeight()+": bmp.getWidth = "+ bmp.getWidth());
		// 调整imageView的高度
		int imageHeight = getWidth() * bmp.getHeight() / bmp.getWidth();
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				350, imageHeight);
		imageView.setLayoutParams(lp);

		// onActivityResult无法触发动画，此处post处理
		/*allLayout.postDelayed(new Runnable() {
			@Override
			public void run() {*/
				allLayout.addView(imageLayout, index);
		/*	}
		}, 200);*/
	}
	/**
	 * 在特定位置添加ImageView
	 */
	private void addImageViewAtIndex(final int index, Bitmap bmp,
			String imagePath) {
		if (bmp == null){
			Log.d("Danny", "addImageViewAtIndex...");
		}
		final RelativeLayout imageLayout = createImageLayout();
		DataImageView imageView = (DataImageView) imageLayout
				.findViewById(R.id.edit_imageView);
		imageView.setImageBitmap(bmp);
		imageView.setBitmap(bmp);
		imageView.setAbsolutePath(imagePath);
         Log.d("Danny", "bmp.getHeight() = "+bmp.getHeight()+": bmp.getWidth = "+ bmp.getWidth());
		// 调整imageView的高度
		int imageHeight = getWidth() * bmp.getHeight() / bmp.getWidth();
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				350, imageHeight);
		imageView.setLayoutParams(lp);

		// onActivityResult无法触发动画，此处post处理
		allLayout.postDelayed(new Runnable() {
			@Override
			public void run() {
				allLayout.addView(imageLayout, index);
			}
		}, 200);
	}

	/**
	 * 根据view的宽度，动态缩放bitmap尺寸
	 * 
	 * @param width
	 *            view的宽度
	 */
	private Bitmap getScaledBitmap(String filePath, int width) {
        Log.d("Danny", "the filepath =  "+filePath+": width =="+width);
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, options);
		int sampleSize = options.outWidth > width ? options.outWidth / width
				+ 1 : 1;
		options.inJustDecodeBounds = false;
		options.inSampleSize = sampleSize;
		return BitmapFactory.decodeFile(filePath, options);
	}

	/**
	 * 初始化transition动画
	 */
	private void setupLayoutTransitions() {
		mTransitioner = new LayoutTransition();
		allLayout.setLayoutTransition(mTransitioner);
		mTransitioner.addTransitionListener(new TransitionListener() {

			@Override
			public void startTransition(LayoutTransition transition,
					ViewGroup container, View view, int transitionType) {
				Log.d("Danny", "startTransition......");

			}

			@Override
			public void endTransition(LayoutTransition transition,
					ViewGroup container, View view, int transitionType) {
				Log.d("Danny", "endTransition");
				if (!transition.isRunning()
						&& transitionType == LayoutTransition.CHANGE_DISAPPEARING) {
					// transition动画结束，合并EditText
					 mergeEditText();
				}
			}
		});
		mTransitioner.setDuration(300);
	}

	/**
	 * 图片删除的时候，如果上下方都是EditText，则合并处理
	 */
	private void mergeEditText() {
		View preView = allLayout.getChildAt(disappearingImageIndex - 1);
		View nextView = allLayout.getChildAt(disappearingImageIndex);
		if (preView != null && preView instanceof EditText && null != nextView
				&& nextView instanceof EditText) {
			Log.d("Danny", "合并EditText");
			EditText preEdit = (EditText) preView;
			EditText nextEdit = (EditText) nextView;
			String str1 = preEdit.getText().toString();
			String str2 = nextEdit.getText().toString();
			String mergeText = "";
			if (str2.length() > 0) {
				mergeText = str1 + "\n" + str2;
				Log.d("Danny", "mergeText===="+mergeText);
			} else {
				mergeText = str1;
			}
           /* Log.d("Danny", "allLayout.getChildCount()="+allLayout.getChildCount());
			int count = allLayout.getChildCount();
			for (int i = 0; i < count;i++){
				Log.d("Danny", "the i = "+i+" :view = "+allLayout.getChildAt(i));
			}*/
			allLayout.setLayoutTransition(null);
			allLayout.removeView(nextView);
			preEdit.setText(mergeText);
			preEdit.requestFocus();
			preEdit.setSelection(mergeText.length(), mergeText.length());
			allLayout.setLayoutTransition(mTransitioner);
		}
	}

	/**
	 * dp和pixel转换
	 * 
	 * @param dipValue
	 *            dp值
	 * @return 像素值
	 */
	public int dip2px(float dipValue) {
		float m = getContext().getResources().getDisplayMetrics().density;
		return (int) (dipValue * m + 0.5f);
	}

	/**
	 * 对外提供的接口, 生成编辑数据上传
	 */
	public List<EditData> buildEditData() {
		List<EditData> dataList = new ArrayList<EditData>();
		int num = allLayout.getChildCount();
		for (int index = 0; index < num; index++) {
			View itemView = allLayout.getChildAt(index);
			EditData itemData = new EditData();
			if (itemView instanceof EditText) {
				EditText item = (EditText) itemView;
				itemData.inputStr = item.getText().toString();
			} else if (itemView instanceof RelativeLayout) {
				DataImageView item = (DataImageView) itemView
						.findViewById(R.id.edit_imageView);
				itemData.imagePath = item.getAbsolutePath();
				itemData.bitmap = item.getBitmap();
			}
			dataList.add(itemData);
		}

		return dataList;
	}

	class EditData {
		String inputStr;
		String imagePath;
		Bitmap bitmap;
	}
}
