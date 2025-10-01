package com.trunganh.bookingbilliards.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.trunganh.bookingbilliards.R
import com.trunganh.bookingbilliards.databinding.ItemTableBinding
import com.trunganh.bookingbilliards.model.Table
import com.trunganh.bookingbilliards.manager.UserManager
import java.text.NumberFormat
import java.util.Locale

class TableAdapter(
    private var tables: List<Table>,
    private val onItemClick: ((Table) -> Unit)? = null,
    private val onBookClick: (Table) -> Unit,
    private val onDeleteClick: ((Table) -> Unit)? = null,
    private val onEditClick: ((Table) -> Unit)? = null
) : RecyclerView.Adapter<TableAdapter.TableViewHolder>() {

    class TableViewHolder(
        private val binding: ItemTableBinding,
        private val onItemClick: ((Table) -> Unit)?,
        private val onBookClick: (Table) -> Unit,
        private val onDeleteClick: ((Table) -> Unit)?,
        private val onEditClick: ((Table) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(table: Table) {
            // Bind dữ liệu vào các view
            binding.apply {
                // Số bàn
                tvTableNumber.text = "Bàn ${table.tableNumber}"
                
                // Tên bàn
                tvName.text = table.name
                
                // Loại bàn
                tvType.text = "Loại: ${table.type}"
                
                // Loại bi
                tvBallType.text = "Bi: ${table.ballType}"
                
                // Giá theo giờ
                val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                tvPricePerHour.text = "${formatter.format(table.pricePerHour)}/giờ"
                
                // Trạng thái bàn
                tvStatus.apply {
                    text = when (table.status.uppercase()) {
                        "AVAILABLE" -> "Trống"
                        "BOOKED" -> "Đã đặt"
                        "IN_USE" -> "Đang sử dụng"
                        else -> "Không xác định"
                    }
                    // Thay đổi màu background tùy theo trạng thái
                    setBackgroundResource(
                        when (table.status.uppercase()) {
                            "AVAILABLE" -> R.drawable.bg_status_available
                            "BOOKED" -> R.drawable.bg_status_booked
                            "IN_USE" -> R.drawable.bg_status_in_use
                            else -> R.drawable.bg_status_unknown
                        }
                    )
                }
                
                // Nội dung mô tả
                tvContent.text = table.content
                
                // Load hình ảnh
                val fullImageUrl = if (table.imageUrl.startsWith("http")) {
                    table.imageUrl
                } else {
                    // Chuẩn hóa đường dẫn tương đối
                    val normalizedPath = table.imageUrl
                        .replace("../", "") // Xóa các phần ../ 
                        .replace("./", "")  // Xóa các phần ./
                        .replace("//", "/") // Xóa các dấu // thừa
                        .trimStart('/')     // Xóa dấu / ở đầu nếu có
                    "http://10.0.2.2:8080/$normalizedPath"
                }
                Log.d("IMAGE_LOADING", "Loading image for table ${table.name}: $fullImageUrl")
                Glide.with(imgTable.context)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.placeholder_table)
                    .error(R.drawable.error_table)
                    .centerCrop()
                    .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(
                            e: com.bumptech.glide.load.engine.GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.e("IMAGE_LOADING", "Failed to load image for table ${table.name}", e)
                            return false
                        }

                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                            dataSource: com.bumptech.glide.load.DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.d("IMAGE_LOADING", "Successfully loaded image for table ${table.name}")
                            return false
                        }
                    })
                    .into(imgTable)
                
                // Hiển thị/ẩn nút xóa và chỉnh sửa dựa vào quyền admin
                btnDelete.visibility = if (UserManager.isAdmin()) android.view.View.VISIBLE else android.view.View.GONE
                btnEdit.visibility = if (UserManager.isAdmin()) android.view.View.VISIBLE else android.view.View.GONE
                
                // Xử lý sự kiện click
                root.setOnClickListener {
                    onItemClick?.invoke(table)
                }

                // Đặt listener cho nút Đặt bàn
                btnBook.setOnClickListener {
                    onBookClick(table)
                }

                // Đặt listener cho nút Xóa
                btnDelete.setOnClickListener {
                    onDeleteClick?.invoke(table)
                }

                // Đặt listener cho nút Chỉnh sửa
                btnEdit.setOnClickListener {
                    onEditClick?.invoke(table)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val binding = ItemTableBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TableViewHolder(binding, onItemClick, onBookClick, onDeleteClick, onEditClick)
    }

    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        holder.bind(tables[position])
    }

    override fun getItemCount() = tables.size

    fun updateData(newTables: List<Table>) {
        tables = newTables
        notifyDataSetChanged()
    }
}
