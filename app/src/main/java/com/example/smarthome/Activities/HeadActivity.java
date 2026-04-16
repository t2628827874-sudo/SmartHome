package com.example.smarthome.Activities;

import android.Manifest;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.smarthome.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 用户头像管理Activity
 * 
 * 功能说明：
 * - 显示当前用户头像
 * - 支持从相册选择图片作为头像
 * - 支持文件格式验证（JPG、PNG、WEBP等）
 * - 支持文件大小限制（最大5MB）
 * - 头像持久化存储
 * 
 * 技术实现：
 * - 使用ActivityResultContracts.PickVisualMedia选择图片（Android 13+推荐方式）
 * - 使用ActivityResultContracts.RequestPermission请求权限（兼容旧版本）
 * - 使用SharedPreferences存储头像路径
 */
public class HeadActivity extends AppCompatActivity {
    private static final String TAG = "HeadActivity";
    
    private Toolbar head_back;
    private Button btn_avatar;
    private ImageView iv_head;
    
    private SharedPreferences sharedPreferences;
    private String currentAccount;
    
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp"};
    
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> pickImageLegacyLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_head);

        initSharedPreferences();
        initLaunchers();
        initView();
        loadSavedAvatar();
        initToolbar();
    }

    /**
     * 初始化SharedPreferences
     * 
     * 获取当前登录账户信息，用于存储该用户的头像
     */
    private void initSharedPreferences() {
        sharedPreferences = getSharedPreferences("userinfo", MODE_PRIVATE);
        currentAccount = sharedPreferences.getString("current_account", "default");
    }

    /**
     * 初始化ActivityResult启动器
     * 
     * 注册三种启动器：
     * 1. pickMediaLauncher - 现代图片选择器（Android 13+）
     * 2. requestPermissionLauncher - 权限请求启动器
     * 3. pickImageLegacyLauncher - 传统图片选择器（兼容旧版本）
     */
    private void initLaunchers() {
        // 现代图片选择器（Android 13+ 推荐方式）
        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        Log.d(TAG, "选择的图片URI: " + uri);
                        handleSelectedImage(uri);
                    } else {
                        Log.d(TAG, "用户取消选择图片");
                    }
                }
        );

        // 权限请求启动器
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "存储权限已授予");
                        openImagePicker();
                    } else {
                        Log.w(TAG, "存储权限被拒绝");
                        Toast.makeText(this, "需要存储权限才能选择头像", Toast.LENGTH_LONG).show();
                    }
                }
        );

        // 传统图片选择器（兼容旧版本）
        pickImageLegacyLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        Log.d(TAG, "传统选择器返回图片URI: " + uri);
                        handleSelectedImage(uri);
                    }
                }
        );
    }

    /**
     * 初始化视图组件
     */
    private void initView() {
        iv_head = findViewById(R.id.iv_head);
        btn_avatar = findViewById(R.id.btn_avatar);
        
        btn_avatar.setOnClickListener(v -> onUploadAvatarClick());
    }

    /**
     * 上传头像按钮点击事件
     * 
     * 执行流程：
     * 1. 检查是否需要请求权限
     * 2. 如需要权限，先请求权限
     * 3. 权限授予后打开图片选择器
     */
    private void onUploadAvatarClick() {
        Log.d(TAG, "用户点击上传头像按钮");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用 PickVisualMedia，无需权限
            openImagePicker();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 使用传统选择器，无需权限
            openImagePickerLegacy();
        } else {
            // Android 10及以下需要请求存储权限
            checkAndRequestPermission();
        }
    }

    /**
     * 检查并请求存储权限
     * 
     * 仅用于 Android 10 及以下版本
     */
    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
            if (checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "已有存储权限");
                openImagePickerLegacy();
            } else {
                Log.d(TAG, "请求存储权限");
                requestPermissionLauncher.launch(permission);
            }
        } else {
            openImagePickerLegacy();
        }
    }

    /**
     * 打开现代图片选择器
     * 
     * 使用 PickVisualMedia API，仅显示图片类型
     */
    private void openImagePicker() {
        Log.d(TAG, "打开现代图片选择器");
        // 使用简单的图片选择方式，兼容不同版本的AndroidX库
        pickMediaLauncher.launch(new PickVisualMediaRequest());
    }

    /**
     * 打开传统图片选择器
     * 
     * 使用 GetContent API，兼容旧版本
     */
    private void openImagePickerLegacy() {
        Log.d(TAG, "打开传统图片选择器");
        pickImageLegacyLauncher.launch("image/*");
    }

    /**
     * 处理选中的图片
     * 
     * 执行流程：
     * 1. 验证文件格式
     * 2. 验证文件大小
     * 3. 保存图片到应用私有目录
     * 4. 更新UI显示
     * 
     * @param uri 选中的图片URI
     */
    private void handleSelectedImage(Uri uri) {
        try {
            // 获取文件信息
            String fileName = getFileName(uri);
            Log.d(TAG, "文件名: " + fileName);
            
            // 验证文件格式
            if (!isValidImageFormat(fileName)) {
                Toast.makeText(this, "不支持的图片格式，请选择JPG、PNG、WEBP等格式", Toast.LENGTH_LONG).show();
                Log.w(TAG, "文件格式不支持: " + fileName);
                return;
            }
            
            // 获取文件大小
            long fileSize = getFileSize(uri);
            Log.d(TAG, "文件大小: " + fileSize + " bytes (" + (fileSize / 1024) + " KB)");
            
            // 验证文件大小
            if (fileSize > MAX_FILE_SIZE) {
                Toast.makeText(this, "图片文件过大，请选择小于5MB的图片", Toast.LENGTH_LONG).show();
                Log.w(TAG, "文件过大: " + (fileSize / 1024 / 1024) + " MB");
                return;
            }
            
            // 保存图片到应用私有目录
            String savedPath = saveImageToPrivateStorage(uri);
            if (savedPath != null) {
                // 保存头像路径到SharedPreferences
                saveAvatarPath(savedPath);
                
                // 更新UI显示
                displayAvatar(savedPath);
                
                Toast.makeText(this, "头像更新成功", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "头像保存成功: " + savedPath);
            } else {
                Toast.makeText(this, "保存头像失败，请重试", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "处理图片失败: " + e.getMessage(), e);
            Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 获取文件名
     * 
     * @param uri 文件URI
     * @return 文件名
     */
    private String getFileName(Uri uri) {
        String fileName = "unknown";
        
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "获取文件名失败: " + e.getMessage());
            }
        } else {
            fileName = uri.getLastPathSegment();
        }
        
        return fileName != null ? fileName : "unknown";
    }

    /**
     * 获取文件大小
     * 
     * @param uri 文件URI
     * @return 文件大小（字节）
     */
    private long getFileSize(Uri uri) {
        long size = 0;
        
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取文件大小失败: " + e.getMessage());
        }
        
        return size;
    }

    /**
     * 验证图片格式
     * 
     * 支持的格式：JPG、JPEG、PNG、WEBP、GIF、BMP
     * 
     * @param fileName 文件名
     * @return 是否为有效的图片格式
     */
    private boolean isValidImageFormat(String fileName) {
        if (fileName == null) return false;
        
        String lowerName = fileName.toLowerCase(Locale.getDefault());
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 保存图片到应用私有目录
     * 
     * 将选中的图片复制到应用的私有存储目录，
     * 文件名包含账户标识，确保不同用户头像独立存储
     * 
     * @param sourceUri 源图片URI
     * @return 保存后的文件路径，失败返回null
     */
    private String saveImageToPrivateStorage(Uri sourceUri) {
        try {
            // 创建应用私有目录
            File avatarDir = new File(getFilesDir(), "avatars");
            if (!avatarDir.exists()) {
                avatarDir.mkdirs();
            }
            
            // 生成唯一文件名
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "avatar_" + currentAccount + "_" + timeStamp + ".jpg";
            File avatarFile = new File(avatarDir, fileName);
            
            // 复制图片数据
            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) {
                Log.e(TAG, "无法打开输入流");
                return null;
            }
            
            // 解码并压缩图片
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            if (bitmap == null) {
                Log.e(TAG, "无法解码图片");
                return null;
            }
            
            // 压缩并保存
            FileOutputStream outputStream = new FileOutputStream(avatarFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
            outputStream.flush();
            outputStream.close();
            bitmap.recycle();
            
            // 删除旧头像文件
            deleteOldAvatar();
            
            return avatarFile.getAbsolutePath();
            
        } catch (Exception e) {
            Log.e(TAG, "保存图片失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 删除旧头像文件
     * 
     * 在保存新头像时，删除该用户的旧头像文件，节省存储空间
     */
    private void deleteOldAvatar() {
        String oldPath = sharedPreferences.getString("avatar_path_" + currentAccount, "");
        if (!oldPath.isEmpty()) {
            File oldFile = new File(oldPath);
            if (oldFile.exists() && oldFile.delete()) {
                Log.d(TAG, "已删除旧头像: " + oldPath);
            }
        }
    }

    /**
     * 保存头像路径到SharedPreferences
     * 
     * @param path 头像文件路径
     */
    private void saveAvatarPath(String path) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("avatar_path_" + currentAccount, path);
        editor.apply();
        Log.d(TAG, "头像路径已保存: " + path);
    }

    /**
     * 加载已保存的头像
     * 
     * 从SharedPreferences读取头像路径并显示
     */
    private void loadSavedAvatar() {
        String savedPath = sharedPreferences.getString("avatar_path_" + currentAccount, "");
        if (!savedPath.isEmpty()) {
            File avatarFile = new File(savedPath);
            if (avatarFile.exists()) {
                displayAvatar(savedPath);
                Log.d(TAG, "加载已保存的头像: " + savedPath);
            }
        }
    }

    /**
     * 显示头像图片
     * 
     * @param imagePath 图片文件路径
     */
    private void displayAvatar(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap != null) {
                    // 缩放图片以适应ImageView
                    int targetWidth = iv_head.getWidth();
                    int targetHeight = iv_head.getHeight();
                    
                    if (targetWidth > 0 && targetHeight > 0) {
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
                        iv_head.setImageBitmap(scaledBitmap);
                        if (scaledBitmap != bitmap) {
                            bitmap.recycle();
                        }
                    } else {
                        iv_head.setImageBitmap(bitmap);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "显示头像失败: " + e.getMessage(), e);
        }
    }

    /**
     * 初始化工具栏
     */
    private void initToolbar() {
        head_back = findViewById(R.id.head_back);
        setSupportActionBar(head_back);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
