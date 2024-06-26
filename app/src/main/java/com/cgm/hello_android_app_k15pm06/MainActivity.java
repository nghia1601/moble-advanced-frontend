package com.cgm.hello_android_app_k15pm06;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cgm.hello_android_app_k15pm06.Cart.CartActivity;
import com.cgm.hello_android_app_k15pm06.Crud.AddProductActivity;
import com.cgm.hello_android_app_k15pm06.Crud.EditProductActivity;
import com.cgm.hello_android_app_k15pm06.entities.Product;
import com.cgm.hello_android_app_k15pm06.service.ProductService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private ListView productListView;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();

    private ProductService productService;

    // Request code for startActivityForResult
    private static final int reload = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        productListView = findViewById(R.id.productListView);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the Up button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        //su ly thong tin tim kiem theo ten
        EditText searchEditText = findViewById(R.id.editTextSearch);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                loadProductList();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Không cần xử lý ở đây
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Khi người dùng thay đổi nội dung của EditText
                // Tải lại danh sách sản phẩm với từ khóa tìm kiếm mới
                searchProduct(s.toString().trim());
            }
        });



        ImageView cartIcon = findViewById(R.id.cartIcon);
        // Gắn sự kiện click cho biểu tượng cart
        cartIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển đến trang cart
                Intent intent = new Intent(MainActivity.this, CartActivity.class);

                startActivity(intent);

            }
        });



        // Initialize Retrofit và ProductService
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.100.8:8080/hello-web-app/rest/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        productService = retrofit.create(ProductService.class);

        // Gọi API để lấy danh sách sản phẩm q
        loadProductList();

        // Đăng ký menu context cho ListView
        registerForContextMenu(productListView);
    }

    private void loadProductList() {
        Call<List<Product>> call = productService.getAllProducts();

        call.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful()) {
                    productList = response.body();
                    // Cập nhật dữ liệu vào ListView
                    updateListView();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load product list", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                // Xử lý lỗi khi không thể tải danh sách sản phẩm
                Toast.makeText(MainActivity.this, "Failed to load product list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateListView() {
        // Tạo adapter mới và gán cho ListView
        productAdapter = new ProductAdapter(productList, this);
        productListView.setAdapter(productAdapter);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.product_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        //info.position chứa vị trí xác định mục được chọn trong listview
        int position = info.position;
        //phương thức get(position) lấy vị trí trong productList
            Product selectedProduct = productList.get(position);
        if (item.getItemId() == R.id.add_product) {
            // Chuyển sang màn hình thêm sản phẩm mới
            Intent intent = new Intent(MainActivity.this, AddProductActivity.class);
            startActivityForResult(intent, reload);
            return true;
        } else if (item.getItemId() == R.id.edit_product) {
            // Chuyển sang màn hình chỉnh sửa sản phẩm và gửi thông tin sản phẩm cần chỉnh sửa
            Intent intent = new Intent(MainActivity.this, EditProductActivity.class);
            intent.putExtra("PRODUCT", selectedProduct);
            //mong đợi kết quả trả về với mã yêu cầu là reload
            startActivityForResult(intent, reload);
            return true;
        } else if (item.getItemId() == R.id.delete_product) {
            // Gọi phương thức để xóa sản phẩm
            deleteProduct(selectedProduct.getId());
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    // Phương thức để xử lý kết quả trả về từ EditProductActivity, addproductactivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == reload && resultCode == RESULT_OK) {
            // Nếu resultCode là RESULT_OK, ta cập nhật lại danh sách sản phẩm
            loadProductList();
        }
    }

    // Phương thức để xóa sản phẩm
    private void deleteProduct(int productId) {
        Call<Void> call = productService.deleteProduct(productId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Xóa sản phẩm thành công
                    Toast.makeText(MainActivity.this, "Product deleted successfully", Toast.LENGTH_SHORT).show();
                    // Load lại danh sách sản phẩm sau khi xóa
                    loadProductList();
                } else {
                    // Xóa sản phẩm thất bại
                    Toast.makeText(MainActivity.this, "Failed to delete product", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Xử lý lỗi khi không thể gửi yêu cầu xóa sản phẩm
                Toast.makeText(MainActivity.this, "Failed to delete product", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchProduct(String keyword) {
        Log.d("Search", "Searching for: " + keyword);
        Call<List<Product>> call = productService.searchProductsByTitle(keyword);
        call.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful()) {
                    productList = response.body();
                    updateListView();
                } else {
                    Toast.makeText(MainActivity.this, "No products found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Failed to search products", Toast.LENGTH_SHORT).show();
                Log.e("Search", "Failed to search for products: " + t.getMessage());
            }
        });
    }



}
